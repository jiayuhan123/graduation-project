package xyz.jiayuhan.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 在系统入口建立并传递 {@code X-Trace-ID}。
 *
 * <p>合法的调用方 TraceId 会继续沿用；异常值会被替换，防止日志注入和超长请求头。</p>
 */
@Component
public class TraceGlobalFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = resolveTraceId(exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER));
        ServerWebExchange tracedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(TRACE_ID_HEADER, traceId)))
                .build();
        tracedExchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        return chain.filter(tracedExchange);
    }

    @Override
    public int getOrder() {
        // 先于鉴权过滤器执行，使鉴权失败响应也拥有 TraceId。
        return -200;
    }

    private String resolveTraceId(String candidate) {
        if (StringUtils.hasText(candidate) && SAFE_TRACE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
