package com.catchyou.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.Collections;

@SpringBootConfiguration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Contact contact() {
        return new Contact("岗位实践第二小组", null, "n34_joe@qq.com");
    }

    @Bean
    public ApiInfo apiInfo(Contact contact) {
        return new ApiInfo(
                "抓到你了——具备安全防护能力的账号系统接口文档",
                null,
                null,
                null,
                contact,
                null,
                null,
                new ArrayList<>()
        );
    }

    @Bean
    public Docket docket(ApiInfo apiInfo,
                         SecurityContext securityContext,
                         SecurityScheme securityScheme) {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.catchyou.controller"))
                .build()
                .securityContexts(Collections.singletonList(securityContext))
                .securitySchemes(Collections.singletonList(securityScheme));
    }

    @Bean
    public SecurityContext securityContext(SecurityReference securityReference) {
        return SecurityContext
                .builder()
                .securityReferences(Collections.singletonList(securityReference))
                .build();
    }

    @Bean
    public SecurityReference securityReference() {
        AuthorizationScope authorizationScope =
                new AuthorizationScope("global", "accessEverything");
        return new SecurityReference("token",
                new AuthorizationScope[]{authorizationScope});
    }

    @Bean
    public SecurityScheme securityScheme() {
        return new ApiKey("token", "token", "header");
    }
}
