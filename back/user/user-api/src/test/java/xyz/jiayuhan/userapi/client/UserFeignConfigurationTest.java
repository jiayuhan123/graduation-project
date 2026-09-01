package xyz.jiayuhan.userapi.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import xyz.jiayuhan.userapi.UserApiConstants;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserFeignConfigurationTest {

    private final UserFeignConfiguration configuration = new UserFeignConfiguration();

    @Test
    void addsConfiguredInternalTokenToEveryUserRequest() {
        RequestInterceptor interceptor = configuration.userInternalTokenRequestInterceptor("shared-secret");
        RequestTemplate requestTemplate = new RequestTemplate();

        interceptor.apply(requestTemplate);

        assertEquals(
                List.of("shared-secret"),
                List.copyOf(requestTemplate.headers().get(UserApiConstants.INTERNAL_TOKEN_HEADER)));
    }

    @Test
    void rejectsCallsWhenInternalTokenIsMissing() {
        RequestInterceptor interceptor = configuration.userInternalTokenRequestInterceptor(" ");

        assertThrows(IllegalStateException.class, () -> interceptor.apply(new RequestTemplate()));
    }
}
