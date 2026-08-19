# common-observability

为 Servlet 请求接续或生成 `X-Trace-ID`，写入响应头及 SLF4J MDC 的 `traceId`。只接受 8—64 位字母、数字、下划线或连字符，非法外部值会被替换，避免日志注入。

Gateway 有独立的 WebFlux 过滤器负责入口 TraceId；下游服务引用本模块后即可继续同一标识。日志格式可通过 `%X{traceId}` 输出该字段。
