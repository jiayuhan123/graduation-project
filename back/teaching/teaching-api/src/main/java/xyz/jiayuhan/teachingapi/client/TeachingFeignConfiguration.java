package xyz.jiayuhan.teachingapi.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.teachingapi.TeachingApiConstants;

/** 仅作用于 {@link TeachingFeignClient} 的内部接口认证配置。 */
public class TeachingFeignConfiguration {

    @Bean
    RequestInterceptor teachingInternalTokenRequestInterceptor(
            @Value("${app.internal-api.token:}") String internalToken) {
        return requestTemplate -> {
            if (!StringUtils.hasText(internalToken)) {
                throw new IllegalStateException("调用教学服务前必须配置 app.internal-api.token");
            }
            requestTemplate.header(TeachingApiConstants.INTERNAL_TOKEN_HEADER, internalToken);
        };
    }
}
