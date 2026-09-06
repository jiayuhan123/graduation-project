package xyz.jiayuhan.teachingservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.jiayuhan.teachingservice.security.TeachingAccessInterceptor;

/** 教学服务的教师权限和学生身份路径配置。 */
@Configuration
@RequiredArgsConstructor
public class TeachingWebConfig implements WebMvcConfigurer {

    private final TeachingAccessInterceptor teachingAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(teachingAccessInterceptor)
                .addPathPatterns(
                        "/api/teaching/teacher/**",
                        "/api/teaching/student/**",
                        "/api/teaching/classes/teaching",
                        "/api/teaching/classes/enrolled",
                        "/api/teaching/assignments/created",
                        "/api/teaching/submissions/mine")
                .order(0);
    }
}
