package com.catchyou.filter;

import com.catchyou.pojo.vo.CommonResult;
import com.catchyou.util.MyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RequestFrequencyFilter extends OncePerRequestFilter {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = (String) request.getAttribute("ip");
        //先看黑名单里有没有这个ip，有的话就直接打回去
        Boolean redisData = redisTemplate.opsForSet().isMember("ip_black_list", ip);
        Assert.notNull(redisData, "Redis数据获取异常");
        if (redisData) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("decisionType", 3);
            MyUtil.setResponse(response, new CommonResult<>(-1, "由于多次的高频访问，您的ip已被锁定", map));
            return;
        }
        //频度检测，同一个ip在一个时间片（5s）内只允许请求最多100次，无论什么接口
        String requestCountKey = ip + "_request_count";
        redisData = redisTemplate.hasKey(requestCountKey);
        Assert.notNull(redisData, "Redis数据获取异常");
        if (!redisData) {
            redisTemplate.opsForValue().set(requestCountKey, "1", 5, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(requestCountKey);
        }
        //如果用户请求接口频率过高，将要求滑块验证
        //此外，如果用户在10分钟内已经是第五次高频请求了，将直接锁定ip
        String redisData2 = redisTemplate.opsForValue().get(requestCountKey);
        Assert.notNull(redisData2, "Redis数据获取异常");
        if (Integer.parseInt(redisData2) > 100) {
            //记录是第几次高频请求
            String blockCountKey = ip + "_block_count";
            redisData = redisTemplate.hasKey(blockCountKey);
            Assert.notNull(redisData, "Redis数据获取异常");
            if (!redisData) {
                redisTemplate.opsForValue().set(blockCountKey, "1", 10, TimeUnit.MINUTES);
            } else {
                redisTemplate.opsForValue().increment(blockCountKey);
            }
            //ip加入黑名单
            redisData2 = redisTemplate.opsForValue().get(blockCountKey);
            Assert.notNull(redisData2, "Redis数据获取异常");
            if (Integer.parseInt(redisData2) >= 5) {
                redisTemplate.opsForSet().add("ip_black_list", ip);
                HashMap<String, Object> map = new HashMap<>();
                map.put("decisionType", 3);
                MyUtil.setResponse(response, new CommonResult<>(-1, "由于多次的高频访问，您的ip已被锁定", map));
                redisTemplate.delete(blockCountKey);
                return;
            }
            HashMap<String, Object> map = new HashMap<>();
            map.put("decisionType", 1);
            MyUtil.setResponse(response, new CommonResult<>(-1, "ip访问过于频繁，需要进行滑块验证", map));
            redisTemplate.delete(requestCountKey);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
