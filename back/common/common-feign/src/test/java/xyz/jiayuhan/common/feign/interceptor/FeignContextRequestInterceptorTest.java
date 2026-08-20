package xyz.jiayuhan.common.feign.interceptor;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import xyz.jiayuhan.common.observability.TraceConstants;
import xyz.jiayuhan.common.security.SecurityHeaders;
import xyz.jiayuhan.common.security.context.UserContext;

import static org.assertj.core.api.Assertions.assertThat;

class FeignContextRequestInterceptorTest {

    private final FeignContextRequestInterceptor interceptor = new FeignContextRequestInterceptor();

    @AfterEach
    void clearContext() {
        UserContext.clear();
        MDC.clear();
    }

    @Test
    void shouldRebuildTrustedContextHeaders() {
        UserContext.setUserId(42L);
        MDC.put(TraceConstants.TRACE_ID_MDC_KEY, "trace-12345678");
        RequestTemplate template = new RequestTemplate();
        template.header(SecurityHeaders.USER_ID, "spoofed-user");
        template.header(TraceConstants.TRACE_ID_HEADER, "spoofed-trace");

        interceptor.apply(template);

        assertThat(template.headers().get(SecurityHeaders.USER_ID)).containsExactly("42");
        assertThat(template.headers().get(TraceConstants.TRACE_ID_HEADER)).containsExactly("trace-12345678");
    }

    @Test
    void shouldRemoveUntrustedHeadersWhenContextIsMissing() {
        RequestTemplate template = new RequestTemplate();
        template.header(SecurityHeaders.USER_ID, "spoofed-user");
        template.header(TraceConstants.TRACE_ID_HEADER, "spoofed-trace");

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKeys(
                SecurityHeaders.USER_ID,
                TraceConstants.TRACE_ID_HEADER);
    }
}
