package xyz.jiayuhan.contentservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.jiayuhan.contentservice.security.ContentAdminAuthorizationInterceptor;

/** 内容服务 Web 路径权限配置。 */
@Configuration
@RequiredArgsConstructor
public class ContentWebConfig implements WebMvcConfigurer {

    private final ContentAdminAuthorizationInterceptor contentAdminAuthorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(contentAdminAuthorizationInterceptor)
                .addPathPatterns("/api/content/admin/**")
                .order(0);
    }
}
