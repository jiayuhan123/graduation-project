package xyz.jiayuhan.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import xyz.jiayuhan.gateway.handler.GlobalExceptionHandler;

@Configuration
public class ExceptionConfig {

    /** 网关兼容组件仍使用 Jackson 2，显式提供统一配置的序列化器。 */
    @Bean
    public ObjectMapper gatewayObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @Order(-1)
    public GlobalExceptionHandler globalExceptionHandler(ObjectMapper objectMapper) {
        return new GlobalExceptionHandler(objectMapper);
    }
}
