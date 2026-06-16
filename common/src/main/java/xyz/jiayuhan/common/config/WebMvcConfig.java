package xyz.jiayuhan.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.jiayuhan.common.interceptor.UserInfoInterceptor;

/**
 * 自动配置并注册 Spring MVC 拦截器
 * ConditionalOnWebApplication 确保只有在 Web 环境下才生效
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册自定义的用户信息提取拦截器，拦截所有路径
        registry.addInterceptor(new UserInfoInterceptor()).addPathPatterns("/**");
    }
}
