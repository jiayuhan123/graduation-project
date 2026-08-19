package xyz.jiayuhan.common.observability;

/** 分布式请求链路标识约定。 */
public final class TraceConstants {

    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private TraceConstants() {
    }
}
