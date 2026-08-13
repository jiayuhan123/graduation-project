package xyz.jiayuhan.common.web.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.jiayuhan.common.web.interceptor.UserInfoInterceptor;

/**
 * Web 公共自动配置
 * <p>
 * 通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 自动装配，业务服务无需手动注册。仅在 Servlet Web 环境下生效。
 * </p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册自定义的用户信息提取拦截器，拦截所有路径
        registry.addInterceptor(new UserInfoInterceptor()).addPathPatterns("/**");
    }
}
