package xyz.jiayuhan.userservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.jiayuhan.userapi.UserApiConstants;
import xyz.jiayuhan.userservice.security.InternalApiAuthenticationInterceptor;

/** 用户服务内部 HTTP 接口的统一 Web 安全边界。 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class InternalApiWebConfig implements WebMvcConfigurer {

    private final InternalApiAuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns(UserApiConstants.INTERNAL_USERS_PATH + "/**");
    }
}
