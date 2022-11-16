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
        // 先看黑名单里有没有这个ip，有的话就直接打回去
        Boolean redisData = redisTemplate.opsForSet().isMember("ip_black_list", ip);
        Assert.notNull(redisData, "Redis数据获取异常");
        if (redisData) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("decisionType", 3);
            MyUtil.setResponse(response, new CommonResult<>(-1, "由于多次的高频访问，您的ip已被锁定", map));
            return;
        }
        // 记录同一个IP在一个时间片（5s）内进行了多少次请求
        String requestCountKey = ip + "_request_count";
        redisData = redisTemplate.hasKey(requestCountKey);
        Assert.notNull(redisData, "Redis数据获取异常");
        if (!redisData) {
            redisTemplate.opsForValue().set(requestCountKey, "1", 5, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(requestCountKey);
        }
        // 如果同一个IP在一个时间片（5s）内进行了超过100次请求，则判断为一次高频访问
        String redisData2 = redisTemplate.opsForValue().get(requestCountKey);
        Assert.notNull(redisData2, "Redis数据获取异常");
        if (Integer.parseInt(redisData2) > 100) {
            // 记录当前IP在十分钟内进行了几次高频访问
            String blockCountKey = ip + "_block_count";
            redisData = redisTemplate.hasKey(blockCountKey);
            Assert.notNull(redisData, "Redis数据获取异常");
            if (!redisData) {
                redisTemplate.opsForValue().set(blockCountKey, "1", 10, TimeUnit.MINUTES);
            } else {
                redisTemplate.opsForValue().increment(blockCountKey);
            }
            // 如果IP在十分钟内进行了超过5次高频访问，则把IP放入黑名单
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
            // 若IP没有被放入黑名单，则本次高频访问后需要进行滑块验证
            HashMap<String, Object> map = new HashMap<>();
            map.put("decisionType", 1);
            MyUtil.setResponse(response, new CommonResult<>(-1, "ip访问过于频繁，需要进行滑块验证", map));
            redisTemplate.delete(requestCountKey);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
