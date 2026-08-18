# 毕业设计微服务基础框架

这是一个可复用于新选题的前后端基础工程。原毕业设计主题及对应业务资料已移除，当前保留网关、用户认证、公共组件、管理端和 UniApp 客户端骨架，后续业务统一在 `dev` 分支扩展。

## 已保留的工程能力

- 后端：Java 21、Spring Boot 4.0.6、Spring Cloud 2025.1.1、Spring Cloud Alibaba 2025.1.0.0
- 基础设施：Nacos、Redis、PostgreSQL、RabbitMQ、MinIO
- 服务：`gateway`、`user-service`、`common-core`、`common-web`、`common-redis`
- 鉴权：Sa-Token + Redis，网关统一校验并传递用户上下文
- 数据访问：MyBatis-Plus + PostgreSQL
- 前端：Vue 3 管理端、UniApp 客户端基础工程
- 本地环境：Docker Compose 中间件编排

## 目录结构

```text
graduation-project/
├── back/                 # Maven 多模块后端
├── front/
│   ├── management/       # Vue 3 管理端
│   └── client-app/       # UniApp 客户端
├── 项目文档/             # 通用技术、架构、接口、部署和测试资料
└── docker-compose.yaml   # 本地中间件
```

## 当前接口

- `POST /api/user/register`：用户注册
- `POST /api/user/login`：用户登录

新选题确定后，应重新补充项目简介、需求文档、领域模型、数据库设计和业务接口，不沿用旧主题的数据结构与业务规则。
