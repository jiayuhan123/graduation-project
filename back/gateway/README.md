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
| `user-service` | `/api/user/**` | `lb://user-service` | 通过 Nacos 发现实例并使用 Spring Cloud LoadBalancer 转发 |

新增服务时，需要同步添加路由，并检查鉴权白名单及本文档。

## 鉴权流程

1. `AuthGlobalFilter` 检查请求路径是否命中 `security.whitelist`。
2. 非白名单请求从 `satoken` 请求头读取令牌。
3. 网关通过 Sa-Token 查询 Redis，获得登录用户 ID。
4. 校验通过后，网关向下游请求写入 `X-User-ID`。
5. 缺少或失效令牌时，返回 HTTP `200` 和业务码 `401`。

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
- Nacos：用于发现 `user-service`；
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

当前配置文件包含固定的远程基础设施信息。部署前应改用环境变量或 Nacos 配置中心，并轮换已经进入配置文件的凭据。

## 安全边界与已知限制

- 下游服务应只允许来自网关或可信内网的流量，避免外部伪造 `X-User-ID`。
- 当前跨域范围过宽，生产环境需要设置明确的允许域名。
- 全局过滤器中存在阻塞式 Sa-Token/Redis 调用，并包装在 Reactor 流程中；高并发前应进行压测并评估响应式线程占用。
- 错误响应会包含部分异常消息，生产环境应避免向客户端暴露内部实现或基础设施细节。
