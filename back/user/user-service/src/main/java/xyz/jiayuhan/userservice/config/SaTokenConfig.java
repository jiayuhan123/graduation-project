package xyz.jiayuhan.userservice.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 将 Sa-Token 注解鉴权接入 Spring MVC 请求链。 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器扫描控制器上的 @SaCheckLogin、@SaCheckRole 和 @SaCheckPermission 注解。
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }
}
