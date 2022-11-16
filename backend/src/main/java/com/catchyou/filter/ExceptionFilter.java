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
            Throwable rootCause = e instanceof NestedServletException ? ((NestedServletException) e).getRootCause() : e;
            rootCause.printStackTrace();
            request.setAttribute("javax.servlet.error.status_code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            request.setAttribute("javax.servlet.error.message", rootCause.getMessage());
            request.setAttribute("javax.servlet.error.request_uri", request.getServletPath());
            request.setAttribute("javax.servlet.error.exception", rootCause);
            // 把请求转发给BasicErrorController
            request.getRequestDispatcher("/error").forward(request, response);
        }
    }
}
