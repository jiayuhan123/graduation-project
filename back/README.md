# 后端服务说明

本目录是项目的 Java 微服务后端父工程，负责聚合公共组件、API 网关和业务服务，并统一管理依赖与构建配置。

## 模块导航

| 模块 | 类型 | 默认端口 | 职责 | 文档 |
| --- | --- | ---: | --- | --- |
| `common` | 公共组件聚合 | - | 统一响应、Web 基础能力和 Redis 自动配置 | [查看文档](./common/README.md) |
| `gateway` | 可运行服务 | `10000` | 路由、鉴权、跨域、异常处理和接口文档聚合 | [查看文档](./gateway/README.md) |
| `user-service` | 可运行服务 | `8001` | 通用认证、账户、用户管理和 RBAC | [查看文档](./user-service/README.md) |

`common` 下的组件不是独立服务，不能单独启动；`gateway` 和 `user-service` 才是当前可运行的 Spring Boot 应用。

## 请求链路

```text
客户端
  │  satoken: <token>
  ▼
gateway :10000
  ├─ 白名单请求直接放行
  └─ 受保护请求校验 Redis 中的会话，并向下游写入 X-User-ID
          │
          ▼
user-service :8001
  ├─ common-web 提取 X-User-ID、统一处理异常
  ├─ PostgreSQL 持久化用户数据
  └─ common-redis / Sa-Token 持久化会话
```

## 技术基线

| 技术 | 当前版本或约束 |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.1 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| MyBatis-Plus | 3.5.15，使用 Boot 4 Starter |
| Sa-Token | 1.39.0 |
| 数据库 | PostgreSQL 18 |
| 服务注册与发现 | Nacos |
| 会话与缓存 | Redis |

版本由父工程 [`pom.xml`](./pom.xml) 统一管理。新增代码必须使用 `jakarta.*`，服务配置使用 `application.yml` 或 `spring.config.import`，不要新增 `bootstrap.yml`。

## 本地构建

前置条件：JDK 21，以及可访问的 Maven 仓库。项目已经提供 Maven Wrapper，无需单独安装 Maven。

```powershell
cd back
.\mvnw.cmd clean verify
```

Linux 或 macOS 使用：

```bash
cd back
./mvnw clean verify
```

只验证某个服务及其依赖：

```powershell
.\mvnw.cmd -pl gateway -am verify
.\mvnw.cmd -pl user-service -am verify
```

## 启动顺序

1. 准备 Nacos 和 Redis。
2. 为 `user-service` 准备空的 PostgreSQL 数据库；Flyway 自动创建和升级用户/RBAC 表。
3. 启动 `user-service`，确认它已注册到 Nacos。
4. 启动 `gateway`。
5. 通过 `http://localhost:10000` 统一访问后端接口。

推荐直接在 IDE 中运行：

- `xyz.jiayuhan.userservice.UserServiceApplication`
- `xyz.jiayuhan.gateway.GatewayApplication`

也可以先安装公共模块，再通过 Maven 启动指定服务：

```powershell
.\mvnw.cmd install -DskipTests
.\mvnw.cmd -pl user-service spring-boot:run
.\mvnw.cmd -pl gateway spring-boot:run
```

最后两条启动命令需要在两个终端中分别运行。

## 统一约定

### 响应结构

所有接口使用 `common-core` 提供的统一结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

当前实现通常保持 HTTP 状态码为 `200`，由响应体中的 `code` 表示业务结果。客户端必须以业务状态码为判断依据。

### 身份传递

- 客户端在 `satoken` 请求头中携带登录令牌。
- 网关完成登录校验后，将用户 ID 写入 `X-User-ID` 请求头。
- Servlet 业务服务通过 `common-web` 的 `UserContext` 获取当前用户 ID。
- 业务服务不应信任绕过网关、由外部直接传入的 `X-User-ID`。
- 用户服务的管理接口还会在服务内通过 Sa-Token 权限码再次授权。

### 配置安全

`application.yml` 只保留 localhost 开发默认值。生产环境必须通过环境变量或 Nacos 覆盖数据库、Redis 和初始管理员等敏感配置。

## 文档维护约定

修改服务时，应同步更新对应模块的 `README.md`，尤其是以下变化：

- 新增或删除接口、路由和白名单；
- 端口、服务名、请求头或配置项变化；
- 新增数据库、缓存、消息队列或外部服务依赖；
- 启动方式、表结构或已知限制变化。
