package com.catchyou.filter;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.NestedServletException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ExceptionFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            //因为这个catch块是会捕获到controller以及controller直接或间接调用的类抛出来的异常的
            //而这类异常都会变成NestedServletException形式的嵌套异常，所以需要先获取最内层的异常
            //如果不是NestedServletException的话，那么就是过滤器本身抛出的异常
            Throwable rootCause = e instanceof NestedServletException ? ((NestedServletException) e).getRootCause() : e;
            rootCause.printStackTrace();
            request.setAttribute("javax.servlet.error.status_code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            request.setAttribute("javax.servlet.error.message", rootCause.getMessage());
            request.setAttribute("javax.servlet.error.request_uri", request.getServletPath());
            request.setAttribute("javax.servlet.error.exception", rootCause);
            //进行转发，BasicErrorController默认对应的URL为/error
            request.getRequestDispatcher("/error").forward(request, response);
        }
    }
}
