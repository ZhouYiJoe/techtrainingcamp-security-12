package com.catchyou.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@SpringBootConfiguration
@EnableWebMvc
public class CorsConfig implements WebMvcConfigurer {
    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        //配置允许访问的URL
        registry.addMapping("/**")
                //允许携带的Header
                .allowedHeaders("*")
                //允许使用的请求方法
                .allowedMethods("PUT", "POST", "GET", "HEAD", "DELETE", "OPTIONS")
                //允许哪些IP访问本项目
                .allowedOrigins("*")
                //预检间隔时间
                .maxAge(3600)
                //是否发送cookie
                .allowCredentials(true);
    }

    /**
     * 进行全局跨域配置后可能会影响swagger页面等静态资源的访问，需要加入以下配置重新指定静态资源位置
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations(
                "classpath:/META-INF/resources/webjars/");
    }

    /**
     * 进行上面的静态资源配置后还会出现中文乱码问题，下面进行字符编码的配置
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> httpMessageConverter : converters) {
            if (StringHttpMessageConverter.class.isAssignableFrom(httpMessageConverter.getClass())) {
                ((StringHttpMessageConverter) httpMessageConverter).setDefaultCharset(StandardCharsets.UTF_8);
            }
        }
    }
}