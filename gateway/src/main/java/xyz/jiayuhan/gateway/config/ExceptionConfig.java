package xyz.jiayuhan.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import xyz.jiayuhan.gateway.handler.GlobalExceptionHandler;

@Configuration
public class ExceptionConfig {

    @Bean
    @Order(-1)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
