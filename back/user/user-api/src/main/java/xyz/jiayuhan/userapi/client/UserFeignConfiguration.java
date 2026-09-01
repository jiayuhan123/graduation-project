package xyz.jiayuhan.userapi.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.userapi.UserApiConstants;

/** 仅作用于 {@link UserFeignClient} 的内部身份请求头配置。 */
public class UserFeignConfiguration {

    @Bean
    RequestInterceptor userInternalTokenRequestInterceptor(
            @Value("${app.internal-api.token:}") String internalToken) {
        return requestTemplate -> {
            if (!StringUtils.hasText(internalToken)) {
                throw new IllegalStateException("调用用户服务前必须配置 app.internal-api.token");
            }
            requestTemplate.header(UserApiConstants.INTERNAL_TOKEN_HEADER, internalToken);
        };
    }
}
