package xyz.jiayuhan.common.security.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.security.SecurityHeaders;
import xyz.jiayuhan.common.security.config.InternalApiProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalApiAuthenticationInterceptorTest {

    @Test
    void rejectsInternalCallsWhenFeatureIsDisabled() {
        InternalApiProperties properties = properties(false, "secret");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> invoke(properties, "secret"));
        assertEquals(ResultCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
    }

    @Test
    void rejectsMismatchedToken() {
        InternalApiProperties properties = properties(true, "secret");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> invoke(properties, "wrong"));
        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void acceptsMatchingToken() {
        assertTrue(invoke(properties(true, "secret"), "secret"));
    }

    private boolean invoke(InternalApiProperties properties, String suppliedToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityHeaders.INTERNAL_TOKEN, suppliedToken);
        return new InternalApiAuthenticationInterceptor(properties)
                .preHandle(request, new MockHttpServletResponse(), new Object());
    }

    private InternalApiProperties properties(boolean enabled, String token) {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(enabled);
        properties.setToken(token);
        return properties;
    }
}
