package com.catchyou.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringBootConfiguration;

@SpringBootConfiguration
public class ObjectMapperConfig {
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
