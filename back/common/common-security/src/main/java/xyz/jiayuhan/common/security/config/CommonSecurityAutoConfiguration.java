package xyz.jiayuhan.common.security.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.jiayuhan.common.security.interceptor.InternalApiAuthenticationInterceptor;

/** 为模块化单体的内部 HTTP 端点注册共享令牌校验。 */
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
        registry.addInterceptor(new InternalApiAuthenticationInterceptor(internalApiProperties))
                .addPathPatterns("/internal/**")
                .order(-50);
    }
}
