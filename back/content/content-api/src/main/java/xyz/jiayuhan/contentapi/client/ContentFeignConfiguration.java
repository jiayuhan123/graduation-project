package xyz.jiayuhan.contentapi.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.contentapi.ContentApiConstants;

/** 仅作用于 {@link ContentFeignClient} 的内部接口认证配置。 */
public class ContentFeignConfiguration {

    @Bean
    RequestInterceptor contentInternalTokenRequestInterceptor(
            @Value("${app.internal-api.token:}") String internalToken) {
        return requestTemplate -> {
            if (!StringUtils.hasText(internalToken)) {
                throw new IllegalStateException("调用内容服务前必须配置 app.internal-api.token");
            }
            requestTemplate.header(ContentApiConstants.INTERNAL_TOKEN_HEADER, internalToken);
        };
    }
}
