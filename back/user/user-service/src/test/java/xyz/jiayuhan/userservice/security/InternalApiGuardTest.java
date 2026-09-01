package xyz.jiayuhan.userservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userapi.UserApiConstants;
import xyz.jiayuhan.userservice.config.InternalApiProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalApiGuardTest {

    @Test
    void acceptsOnlyConfiguredInternalToken() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(true);
        properties.setToken("server-secret");
        InternalApiGuard guard = new InternalApiGuard(properties);

        assertThatCode(() -> guard.check("server-secret")).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.check("wrong"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.FORBIDDEN.getCode()));
    }

    @Test
    void interceptorReadsTokenFromSharedContractHeader() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(true);
        properties.setToken("server-secret");
        InternalApiAuthenticationInterceptor interceptor = new InternalApiAuthenticationInterceptor(
                new InternalApiGuard(properties));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(UserApiConstants.INTERNAL_TOKEN_HEADER, "server-secret");

        assertThatCode(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .doesNotThrowAnyException();
    }
}
