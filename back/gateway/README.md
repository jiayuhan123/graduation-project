# Gateway API 网关

`gateway` 是后端统一流量入口，负责服务路由、登录校验、用户身份透传、跨域处理、网关异常响应和 Knife4j 文档聚合。

## 服务信息

| 项目 | 当前值 |
| --- | --- |
| Maven 模块 | `xyz.jiayuhan:gateway` |
| 应用名 | `gateway` |
| 默认端口 | `10000` |
| Web 技术栈 | Spring Cloud Gateway WebFlux |
| 注册中心 | Nacos |
| 会话存储 | Redis |
| 启动类 | `xyz.jiayuhan.gateway.GatewayApplication` |

## 当前路由

| 路由 ID | 外部路径 | 目标服务 | 说明 |
| --- | --- | --- | --- |
| `user-service` | `/api/user/**` | `lb://user-service` | 认证、账户、用户和 RBAC |
| `file-service` | `/api/files/**` | `lb://file-service` | 私有文件 |
| `notification-service` | `/api/notifications/**` | `lb://notification-service` | 站内通知 |
| `audit-service` | `/api/audits/**` | `lb://audit-service` | 审计查询 |
| `content-service` | `/api/content/**` | `lb://content-service` | 内容题库查询 |
| `learning-service` | `/api/learning/**` | `lb://learning-service` | 学习档案查询 |
| `teaching-service` | `/api/teaching/**` | `lb://teaching-service` | 教学数据查询 |
| `game-service` | `/api/games/**` | `lb://game-service` | 游戏闯关查询 |

新增服务时，需要同步添加路由，并检查鉴权白名单及本文档。

## 鉴权流程

1. `AuthGlobalFilter` 检查请求路径是否命中 `security.whitelist`。
2. 非白名单请求从 `satoken` 请求头读取令牌。
3. 网关通过 Sa-Token 查询 Redis，获得登录用户 ID。
4. 网关先删除客户端传入的 `X-User-ID`，校验通过后再写入可信用户 ID。
5. 缺少或失效令牌时，返回 HTTP `200` 和业务码 `401`。

`TraceGlobalFilter` 会先生成或校验 `X-Trace-ID`，再把它传给下游并写入响应头；鉴权失败响应同样携带该标识。

当前白名单包括登录、注册、健康检查和接口文档相关路径，实际清单以 [`application.yml`](./src/main/resources/application.yml) 为准。

未登录响应示例：

```json
{
  "code": 401,
  "message": "请先登录",
  "data": null
}
```

## 全局能力

### 跨域

`CorsConfig` 当前允许任意来源模式、请求方法和请求头，并允许携带凭据。该策略便于开发联调；生产环境应收紧为可信前端域名。

### 异常响应

网关使用 WebFlux `WebExceptionHandler` 将异常转换为统一 `Result`：

| 场景 | 业务码 | 提示 |
| --- | ---: | --- |
| 路由不存在 | `404` | 请求路径不存在 |
| 下游不可用 | `503` | 服务不可用 |
| 未授权 | `401` | 未授权的访问 |
| 其他异常 | `500` | 网关内部错误 |

HTTP 状态码当前统一为 `200`，调用方应读取响应体中的业务码。

### 接口文档聚合

Knife4j 使用 Nacos 服务发现模式聚合下游接口文档，并排除网关自身。入口白名单中预留了 `/doc.html`、`/v3/api-docs/**` 等路径。下游服务仍需提供兼容的 OpenAPI 文档端点，才能被成功聚合。

## 运行依赖

- JDK 21；
- Nacos：用于发现所有下游服务；
- Redis：用于读取 Sa-Token 会话；
- 已启动并注册到 Nacos 的下游服务。

网关与用户服务的 Sa-Token 配置必须保持一致，尤其是 Token 名称、Redis 数据库和序列化方式。

## 启动

先在 `back` 目录安装公共模块：

```powershell
.\mvnw.cmd install -DskipTests
.\mvnw.cmd -pl gateway spring-boot:run
```

也可以直接运行 `GatewayApplication`。启动后可检查：

```text
GET http://localhost:10000/actuator/health
```

## 关键配置

| 配置前缀 | 作用 |
| --- | --- |
| `server.port` | 网关监听端口 |
| `spring.cloud.nacos.discovery` | Nacos 注册与发现 |
| `spring.cloud.gateway.server.webflux.routes` | 路由表 |
| `spring.data.redis` | Redis 连接 |
| `sa-token` | 令牌名称、超时和并发策略 |
| `security.whitelist` | 无需登录即可访问的路径 |
| `knife4j.gateway` | 接口文档聚合策略 |

配置文件使用 `NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 和 `REDIS_DATABASE` 环境变量，并可由 Nacos 配置覆盖。生产环境不得沿用本地默认密码。

## 安全边界与已知限制

- 网关会过滤客户端伪造的 `X-User-ID`；下游服务仍应只允许来自网关或可信内网的流量。
- 当前跨域范围过宽，生产环境需要设置明确的允许域名。
- Sa-Token/Redis 阻塞校验已调度到 Reactor bounded-elastic 线程池；生产前仍需按目标并发量压测。
- 错误响应会包含部分异常消息，生产环境应避免向客户端暴露内部实现或基础设施细节。
