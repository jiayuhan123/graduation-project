package xyz.jiayuhan.monomer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.jiayuhan.monomer.security.MonolithAuthenticationInterceptor;
import xyz.jiayuhan.monomer.security.MonolithSecurityProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MonolithSecurityProperties.class)
public class MonolithWebConfiguration implements WebMvcConfigurer {

    private final MonolithAuthenticationInterceptor authenticationInterceptor;
    private final MonolithSecurityProperties securityProperties;

    public MonolithWebConfiguration(
            MonolithAuthenticationInterceptor authenticationInterceptor,
            MonolithSecurityProperties securityProperties) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.securityProperties = securityProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(securityProperties.getWhitelist())
                .order(-100);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
