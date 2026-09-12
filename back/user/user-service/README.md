# User Service 用户与身份服务

`user-service` 是单体内的统一身份领域模块，不单独启动或占用端口。它负责账号凭证、Sa-Token 会话、微信身份、学生/教师资料、RBAC、内部用户目录和用户生命周期事件；其他业务数据由对应领域模块维护并统一存入 `monolith_db`。

## 已实现能力

- 用户名、手机号或邮箱 + BCrypt 密码登录；
- 微信小程序 `code2session` 登录、首次自动创建学生账号、绑定与安全解绑；
- 微信开放平台网站应用扫码登录，使用 Redis 一次性 state + HttpOnly Cookie 防止登录 CSRF；
- 小程序和网站分别保存 OpenID，并优先通过 UnionID 关联同一平台账号；
- 纯微信账号首次设置密码；
- Sa-Token + Redis 多端可撤销会话，退出当前会话或全部会话；
- Redis 登录失败计数和临时锁定；
- 学生注册时同事务创建账号、学生资料、`student` 角色和注册事件；
- 当前用户公共资料、学生资料、教师资料和密码维护；
- 管理员用户分页、角色筛选、创建、修改、启禁用、软删除、角色分配和密码重置；
- 一个账号可同时拥有学生、教师、管理员等多个角色，分配角色前校验对应 Profile；
- 数据库存储的 RBAC 角色/权限，并接入 Sa-Token 注解鉴权；
- 受内部 Token 保护的用户摘要、批量目录和用户/角色有效性校验；
- 独立 `user-api` 契约模块，由单体进程内适配器实现，保留未来拆分能力；
- Transactional Outbox、RabbitMQ Publisher Confirm、审计事件和用户生命周期事件；
- PostgreSQL `TIMESTAMPTZ` 对应 Java `OffsetDateTime`，统一按 UTC 写入；
- OpenAPI、Actuator、Prometheus 和自动化测试。

## 角色与权限

内置角色编码：

- `student`：学生；
- `teacher`：教师；
- `admin`：系统管理员。

用户管理接口统一校验 `system:user:manage`，角色权限管理接口统一校验 `system:role:manage`。业务服务继续使用 `learning:*`、`teaching:*`、`game:*` 等权限码，并执行资源归属校验。

## API

### 公开接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/user/register` | 学生密码注册 |
| POST | `/api/user/login` | 用户名、手机号或邮箱登录 |
| POST | `/api/user/login/wechat` | 微信登录；首次登录需提交年级和学期 |
| POST | `/api/user/login/wechat/web/authorize` | 创建网站扫码授权会话并返回微信二维码页地址 |
| POST | `/api/user/login/wechat/web/callback` | 校验 state 并用微信授权 code 完成登录 |

### 当前用户

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/user/logout` | 退出当前会话 |
| POST | `/api/user/logout-all` | 退出全部会话 |
| GET | `/api/user/session` | 查询会话、角色和权限 |
| GET/PUT | `/api/user/me` | 查询或修改公共资料 |
| PUT | `/api/user/me/password` | 校验旧密码后改密 |
| PUT | `/api/user/me/password/initialize` | 微信账号首次设置密码 |
| POST/DELETE | `/api/user/me/wechat` | 绑定或解绑微信 |
| GET/PUT | `/api/user/me/student-profile` | 学生资料 |
| GET/PUT | `/api/user/me/teacher-profile` | 教师资料 |

### 管理端

管理端路径为 `/api/user/admin/users/**` 和 `/api/user/admin/roles/**`，覆盖用户 CRUD、启禁用、密码重置、角色分配、学生/教师资料以及角色权限维护。

### 内部用户目录

内部接口不对普通前端开放，仅允许受信任的 OCR 工作器等调用方携带 `X-Internal-Token`：

- `GET /internal/users/{id}`；
- `POST /internal/users/batch`；
- `GET /internal/users/{id}/validate?role=student`。

响应不包含邮箱、手机号、OpenID、密码或 Token。

服务端由统一 MVC 拦截器保护 `/internal/users/**`，Controller 不再逐方法读取和校验 Token。调用方依赖 `user-api`，通过 `UserFeignClient` 调用；客户端拦截器会统一写入内部 Token。接入示例见 [`user-api/README.md`](../user-api/README.md)。

## 代码结构约定

```text
controller
├─ auth
├─ account
├─ admin
└─ internal

model
├─ request
│  ├─ auth
│  ├─ account
│  └─ admin
└─ response
   ├─ auth
   ├─ account
   └─ admin
```

HTTP 入参统一以 `Request` 结尾，HTTP 出参统一以 `Response` 结尾；数据库对象使用 `entity`。跨服务请求和响应只放在 `user-api`，不放入全局 `common-core`。

## 配置

| 环境变量 | 说明 |
|---|---|
| `WECHAT_ENABLED` | 是否启用微信登录 |
| `WECHAT_APP_ID` | 微信小程序 AppID |
| `WECHAT_APP_SECRET` | 微信小程序 Secret |
| `WECHAT_WEB_ENABLED` | 是否启用微信开放平台网站应用扫码登录 |
| `WECHAT_WEB_APP_ID` | 微信开放平台网站应用 AppID |
| `WECHAT_WEB_APP_SECRET` | 微信开放平台网站应用 AppSecret |
| `WECHAT_WEB_REDIRECT_URI` | 网站应用审核域名下的 Web 回调地址 |
| `WECHAT_WEB_STATE_TTL` | 一次性扫码 state 的有效期，默认 `10m` |
| `INTERNAL_API_ENABLED` | 是否启用内部用户目录 |
| `INTERNAL_API_TOKEN` | 服务间共享的内部接口 Token |
| `BOOTSTRAP_ADMIN_ENABLED` | 是否准备初始管理员 |
| `BOOTSTRAP_ADMIN_USERNAME` | 初始管理员用户名 |
| `BOOTSTRAP_ADMIN_PASSWORD` | 初始管理员密码，必须由环境注入 |

数据库、Redis 和 RabbitMQ 连接由 `monolith-app` 的 `application-server.yml` 与服务器环境变量统一配置。

## 当前数据库说明

Java 代码已经按目标表 `t_user`、`sys_student_profile`、`sys_teacher_profile`、RBAC 和发件箱模型编写。当前结构由 `back/database/init-schema.sql` 统一创建到 `monolith_db`，种子数据由 `back/database/seed-data.sql` 统一管理。

历史小程序账号的 UnionID 无法仅凭已有 OpenID 离线推导。上线后应先让这些用户通过新版小程序登录一次完成 UnionID 回填，再开放 Web 扫码登录。

## 验证

在 `back` 目录执行：

```powershell
.\mvnw.cmd -pl user/user-service -am test
```
