package xyz.jiayuhan.common.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import xyz.jiayuhan.common.observability.TraceConstants;
import xyz.jiayuhan.common.security.SecurityHeaders;
import xyz.jiayuhan.common.security.context.UserContext;

/** 将当前请求的可信身份和 TraceId 传递给下游服务。 */
public final class FeignContextRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        rebuildUserHeader(template);
        rebuildTraceHeader(template);
    }

    private void rebuildUserHeader(RequestTemplate template) {
        template.removeHeader(SecurityHeaders.USER_ID);
        Long userId = UserContext.getUserId();
        if (userId != null) {
            template.header(SecurityHeaders.USER_ID, userId.toString());
        }
    }

    private void rebuildTraceHeader(RequestTemplate template) {
        template.removeHeader(TraceConstants.TRACE_ID_HEADER);
        String traceId = MDC.get(TraceConstants.TRACE_ID_MDC_KEY);
        if (traceId != null && !traceId.isBlank()) {
            template.header(TraceConstants.TRACE_ID_HEADER, traceId);
        }
    }
}
