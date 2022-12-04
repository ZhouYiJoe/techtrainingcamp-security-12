package com.catchyou.config;

import com.catchyou.filter.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@SpringBootConfiguration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private AddressFilter addressFilter;
    @Autowired
    private ExceptionFilter exceptionFilter;
    @Autowired
    private RequestFrequencyFilter requestFrequencyFilter;
    @Autowired
    private TokenFilter tokenFilter;
    @Autowired
    private CorsFilter corsFilter;

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(
                        "/auth/register",
                        "/auth/loginWithUsername",
                        "/auth/loginWithPhone",
                        "/verifyCode/applyCode",
                        "/auth/getPublicKey",
                        "/auth/checkMouseTrack",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v2/**",
                        "/swagger-ui.html/**").permitAll()
                .antMatchers("/**").authenticated();

        httpSecurity.addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestFrequencyFilter, TokenFilter.class)
                .addFilterBefore(addressFilter, RequestFrequencyFilter.class)
                .addFilterBefore(exceptionFilter, AddressFilter.class)
                .addFilterBefore(corsFilter, ExceptionFilter.class);
    }
}
