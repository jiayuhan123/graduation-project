# 后端微服务工程

本目录是 Java 21 / Spring Boot 4 多模块后端。所有外部请求通过 Gateway；每个业务服务独享数据库，跨服务副作用通过 RabbitMQ 领域事件解耦。

## 模块

| 模块 | 端口 | 职责 |
| --- | ---: | --- |
| `gateway` | 10000 | 路由、Token 校验、身份头重建、TraceId、跨域、文档聚合 |
| `user-service` | 8001 | 注册登录、会话、账户、用户管理、RBAC、事务发件箱 |
| `file-service` | 8002 | 阿里云 OSS 私有文件和 PostgreSQL 元数据 |
| `notification-service` | 8003 | 站内通知、注册事件消费和未读状态 |
| `audit-service` | 8004 | 用户与管理操作审计事件持久化 |
| `common/*` | - | 可选公共能力 Jar，不注册为服务 |

## 请求与事件链路

```text
客户端 -> Gateway -> user/file/notification/audit
             |              |
             |              +-> 各服务独立 PostgreSQL 数据库
             +-> Redis 校验 Sa-Token

user-service 业务事务 -> sys_outbox_event -> RabbitMQ topic exchange
                                           +-> notification-service
                                           +-> audit-service
```

网关校验 `satoken` 后删除外部伪造的身份头，再向下游写入 `X-User-ID`。同时建立 `X-Trace-ID`；Servlet 服务会把它写入响应头和日志 MDC。业务服务不得直接暴露到公网。

## 技术基线

| 技术 | 版本/约束 |
| --- | --- |
| Java / Spring Boot | 21 / 4.0.6 |
| Spring Cloud / Alibaba | 2025.1.1 / 2025.1.0.0 |
| MyBatis-Plus | 3.5.15 Boot 4 Starter |
| Sa-Token | 1.39.0 |
| PostgreSQL / RabbitMQ | 18 / 4.3.4 |
| 阿里云 OSS Java SDK V2 | 0.5.1 |

新增代码只使用 `jakarta.*`。版本由父工程 `pom.xml` 管理；第三方依赖先登记到项目依赖版本清单。

## 构建与启动

```powershell
cd back
.\mvnw.cmd clean verify
```

首次本地启动在仓库根目录执行：

```powershell
docker compose up -d postgres redis nacos rabbitmq
```

Compose 只在 PostgreSQL 空数据目录首次初始化时创建 `user_db`、`file_db`、`notification_db` 和 `audit_db`。已有数据目录需由管理员补建数据库。

推荐启动顺序：基础设施 -> `user-service` -> `file-service` / `notification-service` / `audit-service` -> `gateway`。文件服务还必须配置已创建的阿里云 OSS 私有 Bucket：

```powershell
$env:OSS_REGION="cn-hangzhou"
$env:OSS_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"
$env:OSS_ACCESS_KEY_ID="<RAM AccessKey ID>"
$env:OSS_ACCESS_KEY_SECRET="<RAM AccessKey Secret>"
$env:OSS_BUCKET="<private-bucket>"
```

不要把 AccessKey 写入仓库。推荐使用只授予目标 Bucket 必要读写权限的 RAM 身份。

## 统一约定

- JSON 接口返回 `Result<T>`；分页统一返回 `PageResult<T>`，单页上限 100。
- 数据结构由各服务的 Flyway 脚本管理，已执行的迁移文件不得修改。
- 消费者按 `eventId` 幂等；重试耗尽进入服务自己的死信队列。
- RabbitMQ Broker Confirm 成功但数据库状态提交失败时允许重复投递，不承诺“仅一次”。
- 配置使用 `application.yml`、环境变量或 Nacos，不使用 `bootstrap.yml`。
- 生产环境必须替换本地密码、收紧 CORS，并只从网关或可信内网访问业务服务。

详细架构、接口和部署说明见 [项目文档](../项目文档/README.md)。
