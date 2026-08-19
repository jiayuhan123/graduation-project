# Common 公共能力层

`common` 是 Maven 聚合模块，子模块均为按需引用的自动配置 Jar，不独立启动。

| 子模块 | 能力 | 典型使用方 |
| --- | --- | --- |
| `common-core` | `Result`、`PageResult`、错误码、业务异常 | 全部模块 |
| `common-web` | Spring MVC、Jakarta Validation、统一异常处理 | Servlet 服务 |
| `common-security` | `X-User-ID` 解析与 `UserContext` | 受网关保护的 Servlet 服务 |
| `common-data` | PostgreSQL 分页、100 条上限、乐观锁、审计时间填充 | MyBatis-Plus 服务 |
| `common-redis` | Redis JSON 序列化 | 需要 Redis 的服务 |
| `common-openapi` | 每个服务的 OpenAPI 元数据自动配置 | HTTP 业务服务 |
| `common-observability` | `X-Trace-ID` 校验、MDC 与响应头 | Servlet 服务 |
| `common-mq` | 交换机、事件契约、Jackson 3 转换器、可靠发布器 | MQ 生产者/消费者 |

依赖保持单向、能力保持可选。Gateway 是 WebFlux，不能引用 `common-web`、`common-security` 或其他 Servlet 专用模块。事件交换机由 `common-mq` 声明；队列、绑定和死信队列由消费者服务拥有。

```powershell
cd back
.\mvnw.cmd -pl common -am verify
```

各子模块配置和边界见其目录内 `README.md`。
