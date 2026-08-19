# common-web

为 Servlet 服务提供 Spring MVC、Jakarta Validation 和统一异常响应。引用后通过 Spring Boot 自动配置加载 `GlobalExceptionHandler`。

| 异常 | 业务码 |
| --- | ---: |
| `BusinessException` | 异常自带错误码 |
| 请求体或约束校验失败 | 400 |
| 文件超过上传限制 | 2002 |
| 未知异常 | 500，对外隐藏内部堆栈 |

用户身份已独立到 `common-security`，TraceId 已独立到 `common-observability`。需要这些能力的服务应显式引用相应模块，避免 `common-web` 变成不可裁剪的聚合包。

Gateway 基于 WebFlux，不得引用本模块。
