package com.catchyou.filter;

import com.catchyou.util.IpUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AddressFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 获取请求的源IP地址
            String ip = IpUtil.getIpAddr(request);
            // 把IP地址放入请求的属性中
            request.setAttribute("ip", ip);
            // 获取请求的源MAC地址
            String deviceId = IpUtil.getMACAddress(ip);
            // 把MAC地址放入请求的属性中
            if (deviceId == null) deviceId = "局域网内设备";
            request.setAttribute("device_id", deviceId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取MAC地址异常");
        }
        filterChain.doFilter(request, response);
    }
}
