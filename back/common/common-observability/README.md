# common-observability

为 Servlet 请求接续或生成 `X-Trace-ID`，写入响应头及 SLF4J MDC 的 `traceId`。只接受 8—64 位字母、数字、下划线或连字符，非法外部值会被替换，避免日志注入。

`monolith-app` 入口过滤器统一处理 TraceId，各领域模块在同一请求线程上下文中继续使用该标识。日志格式可通过 `%X{traceId}` 输出该字段。
