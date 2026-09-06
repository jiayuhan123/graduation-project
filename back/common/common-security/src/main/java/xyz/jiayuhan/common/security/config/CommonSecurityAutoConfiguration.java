package xyz.jiayuhan.common.security.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.jiayuhan.common.security.interceptor.GatewayUserInterceptor;
import xyz.jiayuhan.common.security.interceptor.InternalApiAuthenticationInterceptor;

/** 在 Servlet 业务服务中自动注册网关身份头拦截器。 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(InternalApiProperties.class)
public class CommonSecurityAutoConfiguration implements WebMvcConfigurer {

    private final InternalApiProperties internalApiProperties;

    public CommonSecurityAutoConfiguration(InternalApiProperties internalApiProperties) {
        this.internalApiProperties = internalApiProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GatewayUserInterceptor()).addPathPatterns("/**").order(-100);
        registry.addInterceptor(new InternalApiAuthenticationInterceptor(internalApiProperties))
                .addPathPatterns("/internal/**")
                .order(-50);
    }
}
