package xyz.jiayuhan.learningapi.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.learningapi.LearningApiConstants;

/** 仅作用于 {@link LearningFeignClient} 的内部接口认证配置。 */
public class LearningFeignConfiguration {

    @Bean
    RequestInterceptor learningInternalTokenRequestInterceptor(
            @Value("${app.internal-api.token:}") String internalToken) {
        return requestTemplate -> {
            if (!StringUtils.hasText(internalToken)) {
                throw new IllegalStateException("调用学习服务前必须配置 app.internal-api.token");
            }
            requestTemplate.header(LearningApiConstants.INTERNAL_TOKEN_HEADER, internalToken);
        };
    }
}
