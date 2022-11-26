package com.catchyou.filter;

import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import com.catchyou.constant.RedisConstants;
import com.catchyou.pojo.dto.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TokenFilter extends OncePerRequestFilter {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 前端自动发送的OPTIONS请求不需要验证token
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(null, null, null));
            filterChain.doFilter(request, response);
            return;
        }
        try {
            // 从请求的头部中获取token，并验证token的有效性
            String token = request.getHeader("token");
            Assert.isTrue(StringUtils.hasText(token), "无效的token");
            JWT jwt = JWTUtil.parseToken(token);
            JWTValidator jwtValidator = JWTValidator.of(jwt);
            // 验证token是否过期
            jwtValidator.validateDate();
            String userId = (String) jwt.getPayload("userId");
            Assert.notNull(userId, "无效的token");
            // 验证该token与Redis中存储的token是否相同
            String rightToken = (String) redisTemplate.opsForHash().get(
                    String.format(RedisConstants.LOGIN_STATE_KEY, userId), "token");
            Assert.isTrue(token.equals(rightToken), "无效的token");
            String jsonStr = (String) redisTemplate.opsForHash().get(
                    String.format(RedisConstants.LOGIN_STATE_KEY, userId), "login_user");
            Assert.notNull(jsonStr, "无效的token");
            // 把用户的登录状态保存到SecurityContext中
            LoginUser loginUser = JSONUtil.toBean(jsonStr, LoginUser.class);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginUser, null, null);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
        }
    }
}
