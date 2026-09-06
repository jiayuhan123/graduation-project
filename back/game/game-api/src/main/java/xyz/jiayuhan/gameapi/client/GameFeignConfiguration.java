package xyz.jiayuhan.gameapi.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.gameapi.GameApiConstants;

/** 仅作用于 {@link GameFeignClient} 的内部接口认证配置。 */
public class GameFeignConfiguration {

    @Bean
    RequestInterceptor gameInternalTokenRequestInterceptor(
            @Value("${app.internal-api.token:}") String internalToken) {
        return requestTemplate -> {
            if (!StringUtils.hasText(internalToken)) {
                throw new IllegalStateException("调用游戏服务前必须配置 app.internal-api.token");
            }
            requestTemplate.header(GameApiConstants.INTERNAL_TOKEN_HEADER, internalToken);
        };
    }
}
