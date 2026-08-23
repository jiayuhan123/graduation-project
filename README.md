# 毕业设计微服务基础框架

这是一个可复用于新选题的微服务基础工程。后端已具备标准用户认证、网关、文件、通知、审计、消息可靠投递和公共能力层；具体毕业设计业务可以在此基础上按领域边界继续扩展。

## 已保留的工程能力

- 后端：Java 21、Spring Boot 4.0.6、Spring Cloud 2025.1.1、Spring Cloud Alibaba 2025.1.0.0
- 基础设施：Nacos、Redis、PostgreSQL 18、RabbitMQ 4.3.4、阿里云 OSS
- 服务：`gateway`、`user-service`、`file-service`、`notification-service`、`audit-service`
- 公共层：统一结果/错误码、数据访问、安全上下文、OpenAPI、TraceId、Redis、RabbitMQ
- 鉴权：Sa-Token + Redis，网关统一校验并传递用户上下文
- 数据访问：MyBatis-Plus + PostgreSQL
- 可靠消息：事务发件箱、Publisher Confirm、指数退避、消费幂等和死信队列
- 前端：暂缓开发，仅保留 Vue 3 管理端基础工程
- 本地环境：Docker Compose 中间件编排

## 目录结构

```text
graduation-project/
├── back/                 # Maven 多模块后端
├── front/
│   └── management/       # 保留但暂缓开发的 Vue 3 管理端
├── primary-math-platform-feature-plan.docx  # 当前初步功能规划
└── docker-compose.yaml   # 本地中间件
```

## 通用后端能力

- 认证：注册、用户名/手机号/邮箱登录、退出、会话查询；
- 账户：个人资料查询与修改、安全改密；
- 管理：用户分页、创建、禁用、软删除、重置密码；
- 权限：数据库 RBAC、角色与权限分配、服务内 Sa-Token 授权；
- 工程：Flyway 自动迁移、OpenAPI 3 文档和自动化测试。
- 文件：阿里云 OSS 私有 Bucket，上传、下载、元数据、所有权校验和逻辑删除；
- 通知：消费注册事件生成站内信，支持分页、未读数和已读状态；
- 审计：异步持久化用户操作事件，支持当前用户审计记录查询；
- 可观测性：网关生成并全链路传递 `X-Trace-ID`。

完整文档见 [项目技术文档索引](./项目文档/README.md)，后端模块说明见 [back/README.md](./back/README.md)。

新选题确定后，应重新补充项目简介、需求文档、领域模型、数据库设计和业务接口，不沿用旧主题的数据结构与业务规则。
