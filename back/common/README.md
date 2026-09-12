# Common 公共能力层

`common` 是 Maven 聚合模块，子模块均为按需引用的自动配置 Jar，不独立启动。

| 子模块 | 能力 | 典型使用方 |
| --- | --- | --- |
| `common-core` | `Result`、`PageResult`、错误码、业务异常 | 全部模块 |
| `common-web` | Spring MVC、Jakarta Validation、统一异常处理 | Web 领域模块 |
| `common-security` | Sa-Token、`UserContext` 与 `/internal/**` 共享令牌校验 | 单体入口与领域模块 |
| `common-data` | PostgreSQL 分页、JSONB、实体审计基类、乐观锁和 UTC 时间填充 | MyBatis-Plus 领域模块 |
| `common-redis` | Redis JSON 序列化 | 需要 Redis 的模块 |
| `common-openapi` | 单体 OpenAPI 元数据自动配置 | `monolith-app` |
| `common-observability` | `X-Trace-ID` 校验、MDC 与响应头 | `monolith-app` |
| `common-mq` | 交换机、事件契约、Jackson 3 转换器、可靠发布器 | MQ 生产者/消费者 |

依赖保持单向、能力保持可选。HTTP 鉴权和追踪统一由 `monolith-app` 入口处理；事件交换机由 `common-mq` 声明，队列、绑定和死信队列仍由对应消费模块拥有。

```powershell
cd back
.\mvnw.cmd -pl common -am verify
```

各子模块配置和边界见其目录内 `README.md`。
