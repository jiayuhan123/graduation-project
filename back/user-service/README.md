# User Service 通用用户服务

`user-service` 是与毕业设计具体选题无关的账号与权限基础服务，提供注册登录、会话管理、个人资料、密码修改、用户管理和 RBAC。业务选题确定后，新业务服务只引用用户 ID 并按权限码授权，不把领域数据继续堆入本服务。

## 已实现能力

- 用户名、手机号或邮箱登录，BCrypt 哈希校验；
- Sa-Token + Redis 可撤销会话，登录、退出和会话查询；
- 注册用户自动分配内置 `user` 角色；
- 当前用户资料查询/修改和安全改密；
- 管理员分页查询、创建、修改、禁用、软删除用户；
- 管理员分配角色、重置密码，敏感操作后强制目标账号下线；
- 数据库存储的 RBAC 角色/权限，并接入 Sa-Token 注解鉴权；
- Redis 登录失败计数，默认 15 分钟内失败 5 次后暂时锁定；
- Flyway 自动建表与升级、MyBatis-Plus 分页/乐观锁/逻辑删除；
- OpenAPI 3 文档和基础自动化测试。
- 业务数据与领域事件同事务写入 `sys_outbox_event`；
- 后台批量派发事件，支持 RabbitMQ Publisher Confirm、失败退避与并发实例安全领取；
- 发布注册事件和用户/RBAC 管理审计事件，供通知、审计服务异步消费。

## 技术与端点

| 项目 | 值 |
|---|---|
| 应用名 | `user-service` |
| 默认端口 | `8001` |
| Java / Spring Boot | Java 21 / Spring Boot 4.0.6 |
| 数据库 | PostgreSQL 18 + MyBatis-Plus 3.5.15 + Flyway |
| 会话 | Sa-Token 1.39.0 + Redis |
| 消息 | RabbitMQ + Transactional Outbox |
| 文档 | `/swagger-ui.html`、`/v3/api-docs` |
| 健康检查 | `/actuator/health` |

外部调用统一经过 Gateway `:10000`。登录成功后，客户端使用响应中的 `tokenName` 作为请求头名称，默认形式为：

```http
satoken: <token>
```

## API 一览

### 公开接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/user/register` | 注册，保持原前端请求结构兼容 |
| POST | `/api/user/login` | 用户名、手机号或邮箱登录 |

### 登录用户接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/user/logout` | 退出当前会话 |
| GET | `/api/user/session` | 查询当前会话、角色和权限 |
| GET | `/api/user/me` | 查询当前用户资料 |
| PUT | `/api/user/me` | 修改可编辑资料 |
| PUT | `/api/user/me/password` | 修改密码并使该账号全部会话失效 |

### 管理接口

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/api/user/admin/users` | `system:user:list` |
| GET | `/api/user/admin/users/{id}` | `system:user:query` |
| POST | `/api/user/admin/users` | `system:user:create` |
| PUT | `/api/user/admin/users/{id}` | `system:user:update` |
| PUT | `/api/user/admin/users/{id}/status` | `system:user:status` |
| PUT | `/api/user/admin/users/{id}/roles` | `system:user:role` |
| PUT | `/api/user/admin/users/{id}/password` | `system:user:password-reset` |
| DELETE | `/api/user/admin/users/{id}` | `system:user:delete` |
| GET/POST | `/api/user/admin/roles` | `system:role:list/create` |
| PUT/DELETE | `/api/user/admin/roles/{id}` | `system:role:update/delete` |
| PUT | `/api/user/admin/roles/{id}/status` | `system:role:update` |
| PUT | `/api/user/admin/roles/{id}/permissions` | `system:role:permission` |
| GET | `/api/user/admin/permissions` | `system:permission:list` |

请求、响应和完整示例见 [用户服务接口文档](../../项目文档/5-接口文档/用户服务接口.md)。

## 兼容性约定

原有客户端不需要修改即可继续使用：

- 登录仍为 `POST /api/user/login`，请求字段仍为 `username`、`password`；
- 注册仍为 `POST /api/user/register`，原字段 `username`、`password`、`realName`、`phone` 均保留；
- 登录响应继续返回 `token`、`tokenName`、`userId`、`username`、`role`、`expireAt`；
- 新增的 `roles`、`permissions`、`timeout` 字段可由旧客户端忽略。

注册新增可选字段 `nickname`、`email`，手机号也允许为空，以适应尚未确定的新业务。

## 数据模型与迁移

启动时 Flyway 自动执行 `src/main/resources/db/migration`。首次启动会创建：

| 表 | 用途 |
|---|---|
| `t_user` | 账号、资料、状态、登录审计与逻辑删除 |
| `sys_role` | 角色字典 |
| `sys_permission` | 权限字典 |
| `sys_user_role` | 用户角色关联 |
| `sys_role_permission` | 角色权限关联 |
| `flyway_schema_history` | Flyway 迁移历史 |
| `sys_outbox_event` | 领域事件事务发件箱、状态与重试信息 |

迁移脚本会保留早期 `t_user` 数据并补充缺失字段。用户名和邮箱统一按小写保存；用户名、手机号、邮箱使用“未删除记录”唯一索引。内置角色：

- `user`：注册账号默认角色；
- `admin`：拥有当前全部系统管理权限。

不要手工修改已经在环境中执行过的迁移文件。后续变更必须继续新增更高版本迁移。

## 创建初始管理员

仓库不保存默认管理员密码。首次部署时临时设置：

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED="true"
$env:BOOTSTRAP_ADMIN_USERNAME="admin"
$env:BOOTSTRAP_ADMIN_PASSWORD="请替换为强密码"
./mvnw.cmd -pl user-service spring-boot:run
```

初始化器会创建账号或把已有同名账号分配为 `admin`。完成后关闭 `BOOTSTRAP_ADMIN_ENABLED`；重复开启不会重置已有密码。

## 配置

本地默认连接 `localhost` 上由根目录 Compose 提供的服务，生产环境必须覆盖敏感项：

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `SERVER_PORT` | `8001` | 服务端口 |
| `NACOS_SERVER_ADDR` | `localhost:8848` | Nacos 地址 |
| `DB_URL` | `jdbc:postgresql://localhost:5432/user_db` | JDBC URL |
| `DB_USERNAME` | `root` | 数据库账号 |
| `DB_PASSWORD` | `111111` | 仅本地默认密码 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `REDIS_PASSWORD` | `111111` | 仅本地默认密码 |
| `SA_TOKEN_TIMEOUT` | `604800` | 会话最长有效期，秒 |
| `SA_TOKEN_ACTIVE_TIMEOUT` | `86400` | 无操作有效期，秒 |
| `BCRYPT_STRENGTH` | `12` | BCrypt cost |
| `LOGIN_MAX_ATTEMPTS` | `5` | 锁定前失败次数 |
| `LOGIN_LOCK_DURATION` | `15m` | 登录失败计数有效期 |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | RabbitMQ 地址 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `root` / `111111` | 仅本地默认凭据 |
| `MQ_CONFIRM_TIMEOUT` | `5s` | 等待 Broker Confirm 的时限 |
| `OUTBOX_FIXED_DELAY_MS` | `5000` | 发件箱扫描间隔 |
| `OUTBOX_BATCH_SIZE` | `50` | 每批领取数量 |
| `OUTBOX_MAX_ATTEMPTS` | `10` | 转为 FAILED 前最大尝试数 |
| `OUTBOX_RETENTION_DAYS` | `7` | 已发布事件保留天数，最小 1 |
| `OUTBOX_CLEANUP_CRON` | `0 0 3 * * *` | 成功历史清理计划 |

`application.yml` 使用 `spring.config.import=optional:nacos:user-service.yaml`，因此环境变量、本地配置和 Nacos 配置可以按 Spring 优先级覆盖。

## 本地运行与验证

在项目根目录启动基础中间件：

```powershell
docker compose up -d postgres redis nacos rabbitmq
```

然后在 `back` 目录执行：

```powershell
./mvnw.cmd -pl user-service -am test
./mvnw.cmd -pl user-service spring-boot:run
```

Docker 未启动时仍可执行单元测试；应用集成启动需要 PostgreSQL、Redis、Nacos 和 RabbitMQ。

## 可靠事件说明

注册和管理操作不会在业务事务中直接调用 RabbitMQ。业务表与发件箱先在同一 PostgreSQL 事务提交，再由调度器使用 `FOR UPDATE SKIP LOCKED` 领取待发布行。Broker Confirm 成功后记录 `PUBLISHED`；失败按指数退避，达到上限标记 `FAILED` 供运维处理。

该方案保证“业务提交后事件最终可重试”，但网络与事务边界决定消息可能重复，因此下游必须用 `eventId` 唯一约束实现幂等。

## 安全边界

- 网关会删除客户端伪造的 `X-User-ID`，校验 Token 后再重建；
- 用户服务自身通过 Sa-Token 注解再次校验登录和权限，不能只依赖网关；
- 密码只保存 BCrypt 哈希，不进入响应、缓存 Key 或业务日志；
- 登录失败对“不存在账号”和“密码错误”返回相同错误；
- 禁用、删除、改密、角色调整和管理员重置密码都会让相关会话失效；
- 管理员不能禁用、删除、重置自身密码或修改自身角色，避免误锁；
- `admin`、`user` 内置角色不能禁用、删除或重分配权限。

忘记密码流程没有伪造一个不安全的“直接重置”接口。选题确定并接入邮件或短信供应商后，应增加一次性验证码、有效期、次数限制和审计，再开放自助找回。
