package xyz.jiayuhan.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 网关全局跨域配置类
 * 处理前后端分离架构下的跨域问题
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许任何 HTTP 方法（GET, POST, PUT, DELETE, OPTIONS 等）
        config.addAllowedMethod("*");
        
        // 允许任何来源的跨域请求。在 Spring Boot 2.4 / Spring 5.3 之后，如果 setAllowCredentials 为 true，需使用 addAllowedOriginPattern
        config.addAllowedOriginPattern("*");
        
        // 允许任何请求头
        config.addAllowedHeader("*");
        
        // 允许前端请求携带凭证（例如 Cookie）
        config.setAllowCredentials(true); 

        // 针对所有的路径应用上述的跨域配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
