# 后端微服务工程

本目录是 Java 21 / Spring Boot 4 多模块后端。所有外部请求通过 Gateway；每个业务服务独享数据库，跨服务副作用通过 RabbitMQ 领域事件解耦。

## 模块

| 模块 | 端口 | 职责 |
| --- | ---: | --- |
| `gateway` | 10000 | 路由、Token 校验、身份头重建、TraceId、跨域、文档聚合 |
| `user/user-api` | - | 用户服务内部 HTTP 契约、OpenFeign 客户端和服务间 Token 注入 |
| `user/user-service` | 8001 | 注册登录、会话、账户、用户管理、RBAC、事务发件箱 |
| `file-service` | 8002 | 阿里云 OSS 私有文件和 PostgreSQL 元数据 |
| `notification-service` | 8003 | 站内通知、注册事件消费和未读状态 |
| `audit-service` | 8004 | 用户与管理操作审计事件持久化 |
| `content/content-api` | - | 题目、知识点内部查询契约与 OpenFeign 客户端 |
| `content/content-service` | 8005 | 内容模块、知识点、题目和错因字典的管理、审核、查询与可靠审计 |
| `learning/learning-api` | - | 练习任务内部查询契约与 OpenFeign 客户端 |
| `learning/learning-service` | 8006 | 固定题库练习、作答、诊断、掌握度、推荐、学习报告及作业进度可靠回写 |
| `teaching/teaching-api` | - | 作业内部查询契约与 OpenFeign 客户端 |
| `teaching/teaching-service` | 8007 | 班级成员、统一/分层作业、发布快照、提交汇总与可靠审计 |
| `game/game-api` | - | 游戏对局内部查询契约与 OpenFeign 客户端 |
| `game/game-service` | 8008 | 关卡、单人/团队对局、进度和奖励的持久层及查询接口 |
| `common/*` | - | 可选公共能力 Jar，不注册为服务 |

## 请求与事件链路

```text
客户端 -> Gateway -> user/content/learning/teaching/game/file/notification/audit
             |                            |
             |                            +-> 各服务独立 PostgreSQL 数据库
             +-> Redis 校验 Sa-Token

user-service 业务事务 -> sys_outbox_event -> RabbitMQ topic exchange
                                           +-> notification-service
                                           +-> audit-service

其他业务服务 -> UserFeignClient -> Nacos + LoadBalancer -> user-service /internal/users/**

learning-service -> content-service / teaching-service / user-service
teaching-service / game-service -> learning-service
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

本地调试统一连接共享开发服务器上的 PostgreSQL、Redis、Nacos 和 RabbitMQ，无需启动本地基础设施。连接地址和凭据统一保存在仓库根目录的 `.env` 中；该文件已被 Git 忽略，禁止把真实密码复制进 `application-local.yml`。若需要离线开发或隔离环境，可在仓库根目录自行启动本地组件，并修改 `.env` 中的连接地址：

```powershell
docker compose up -d postgres redis nacos rabbitmq
```

推荐使用统一脚本启动本地服务。脚本会加载根目录 `.env`、检查当前服务需要的变量、构建模块，并以 `local` 环境启动：

```powershell
.\back\run-local-service.ps1 -Service user-service -CheckOnly
.\back\run-local-service.ps1 -Service user-service
```

已经构建过时可以增加 `-SkipBuild`。其他服务把 `user-service` 替换为 `gateway`、`content-service`、`learning-service`、`teaching-service`、`game-service`、`file-service`、`notification-service` 或 `audit-service`。本地环境是默认 profile；从仓库根目录打开 IDEA 并直接运行 Spring Boot 主类时，服务会读取根目录 `.env`。测试和生产启动必须显式激活 `test` 或 `prod`。

- `application.yml`：端口、业务参数、监控等环境无关配置。
- `application-local.yml`：本地联调配置；共享服务器连接地址和凭据全部由根目录 `.env` 注入，Nacos 配置导入可选。
- `application-test.yml`：关闭 Nacos，测试所需外部连接由测试代码或 CI 注入。
- `application-prod.yml`：Nacos 导入为强制，数据库、Redis、RabbitMQ 和 Nacos 凭据均无默认值；缺少环境变量时启动失败。

生产变量可参考仓库根目录的 `.env.prod.example`，真实密码不得提交到 Git。

Compose 只在 PostgreSQL 空数据目录首次初始化时创建 `user_db`、`content_db`、`learning_db`、`teaching_db`、`game_db`、`file_db`、`notification_db` 和 `audit_db`。已有数据目录需由管理员补建数据库。

推荐启动顺序：基础设施 -> `user-service` / `content-service` / `file-service` -> `teaching-service` / `notification-service` / `audit-service` -> `learning-service` -> `game-service` -> `gateway`。学习服务创建教师作业练习时需要查询教学服务，并通过本地事务发件箱重试回写作业进度。文件服务还必须配置已创建的阿里云 OSS 私有 Bucket：

```powershell
$env:OSS_REGION="cn-hangzhou"
$env:OSS_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"
$env:OSS_ACCESS_KEY_ID="<RAM AccessKey ID>"
$env:OSS_ACCESS_KEY_SECRET="<RAM AccessKey Secret>"
$env:OSS_BUCKET="<private-bucket>"
```

不要把 AccessKey 写入仓库。推荐使用只授予目标 Bucket 必要读写权限的 RAM 身份。

## 统一约定

- JSON 接口返回 `Result<T>`；错误响应同时使用真实 HTTP 状态（4xx/5xx），分页统一返回 `PageResult<T>`，单页上限 100。
- HTTP 入参使用 `*Request`，出参使用 `*Response`；跨服务契约归属对应的 `*-api` 模块，不放入全局公共模块。
- 当前不启用自动数据库迁移；首次运行前需按项目建表文档手工创建并核对数据库结构。
- 消费者按 `eventId` 幂等；重试耗尽进入服务自己的死信队列。
- RabbitMQ Broker Confirm 成功但数据库状态提交失败时允许重复投递，不承诺“仅一次”。
- 配置使用 `application-{profile}.yml`、环境变量或 Nacos，不使用 `bootstrap.yml`。
- 生产环境必须替换本地密码、收紧 CORS，并只从网关或可信内网访问业务服务。

详细架构、接口和部署说明见 [项目文档](../项目文档/README.md)。
