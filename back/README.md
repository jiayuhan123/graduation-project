# 模块化单体后端

本目录是项目默认且唯一维护的模块化单体后端。当前使用一个 JVM、一个 PostgreSQL 数据库和一个 HTTP 端口跑通完整业务，同时保留清晰的领域模块边界，答辩后仍可按模块重新拆分。

旧微服务实现不在当前分支继续维护，需要对照或回退时可查看 Git 历史中的 `dev` 分支。

## 当前结构

```text
back/
├─ monolith-app/          唯一启动入口、统一鉴权、跨模块本地适配器
├─ common/                通用核心、Web、数据、Redis、安全、MQ、可观测性
├─ user/                  用户与权限
├─ content/               教学内容与题库
├─ learning/              学习与练习
├─ teaching/              班级、作业与教学管理
├─ game/                  游戏化学习
├─ file-service/          文件与 OSS
├─ notification-service/  通知与 RabbitMQ 消费
├─ audit-service/         审计与 RabbitMQ 消费
└─ database/              单库建表与种子数据入口
```

各业务模块仍是独立 Maven JAR，但不再各自启动。`monolith-app` 是唯一的 `@SpringBootApplication`，统一装配全部 Controller、Service、Mapper、Redis、RabbitMQ listener 和定时任务。

微服务时期的 Feign 契约与 DTO 暂时保留，运行时由本地适配器直接调用用户、内容、教学模块，不经过 Nacos、负载均衡或 HTTP。外部 `/api/**` 路径保持不变，开发前端统一访问服务器单体地址 `http://122.51.234.113:10000`。

## 开发启动

开发阶段统一使用服务器 PostgreSQL、Redis 和 RabbitMQ，不启动本地基础设施。运行 Java 进程的机器需要 JDK 21。

1. 创建本地配置：

   ```powershell
   Copy-Item .env.example .env
   ```

   修改 `.env` 中的服务器密码和 OSS 配置。仓库根目录已有共享开发环境 `.env` 时，应用也会自动读取。`.env` 已被忽略，不会提交密钥。

2. 构建并测试：

   ```powershell
   .\mvnw.cmd clean verify
   ```

3. 启动单体：

   ```powershell
   & "$env:JAVA_HOME\bin\java.exe" -jar monolith-app\target\monolith-app-0.0.1-SNAPSHOT.jar
   ```

常用入口：

- 业务 API：`http://122.51.234.113:10000/api/**`
- Swagger UI：`http://122.51.234.113:10000/swagger-ui.html`
- 健康检查：`http://122.51.234.113:10000/actuator/health`
- RabbitMQ 管理台：`http://122.51.234.113:15672`

服务器上的 `monolith_db` 已完成初始化。需要在新的服务器环境重建时，可创建 `${MONOLITH_DB_NAME}` 数据库后执行：

```powershell
psql -v ON_ERROR_STOP=1 -d monolith_db -f database\init-schema.sql
psql -v ON_ERROR_STOP=1 -d monolith_db -f database\seed-data.sql
```

## 运行模型

- PostgreSQL：八个领域的表合并到一个数据库；现有表名没有冲突。
- Redis：继续承载 Sa-Token 会话、登录失败限制和微信网页登录状态。
- RabbitMQ：交换机、队列、监听器、发布确认、重试和 outbox 调度均保留。
- 安全：单体入口直接校验 Sa-Token，不再信任原网关写入的 `X-User-ID` 请求头。
- 配置：默认启用 `server` profile，读取当前目录 `.env`；为兼容仓库根目录启动，也会回退读取上一级 `.env`。生产环境可使用 `application-prod.yml` 与环境变量。

## 数据迁移说明

`database/init-schema.sql` 负责新库结构，`database/seed-data.sql` 负责基础数据。服务器 `monolith_db` 已执行这两组脚本并验证 47 张表。

2026-09-11 已使用 `database/tools/ServerDataMigration.java` 将服务器上的 `user_db`、`content_db`、`learning_db`、`teaching_db`、`game_db`、`file_db`、`notification_db` 和 `audit_db` 迁入 `monolith_db`。迁移后重新连接源库与目标库逐表核对，结果为 47 张表、源库 788 行、目标库 788 行。旧版 `content_db` 不存在后来新增的 `question_feedback` 表，目标库保留该空表。

迁移工具提供两个操作：

- `inspect`：只读检查源库/目标库的表结构和行数。
- `migrate`：在目标库单个事务中清空 47 张表并重新复制；源库连接始终只读，任一异常或行数不一致都会整体回滚。

执行工具前需使用 JDK 21，并向进程提供 `DB_HOST`、`POSTGRES_PORT`、`DB_USERNAME`、`DB_PASSWORD`。正式再次执行 `migrate` 前必须先停止旧服务写入，并先运行 `inspect`；该操作会以八个旧库当时的数据完整替换 `monolith_db` 中的数据。
