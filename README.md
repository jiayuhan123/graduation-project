# 面向特定社区的文明祭扫系统设计与实现

#### 介绍
本项目为毕业设计课题《面向特定社区的文明祭扫系统设计与实现》。系统以微服务架构为核心，面向特定社区（小区、街道等）提供线上线下结合的文明祭扫服务，涵盖逝者纪念档案管理、线上祭扫（献花/点烛/留言）、祭扫错峰预约、社区公告宣传等核心功能，倡导绿色、文明、安全的祭扫新风尚。

#### 软件架构
本项目采用前沿微服务架构：
- **核心框架**: Java 21, Spring Boot 4.0.6, Spring Cloud 2025.1.1, Spring Cloud Alibaba 2025.1.0.0
- **数据库层**: PostgreSQL 18, MyBatis-Plus 3.5.15
- **鉴权与中间件**: Sa-Token 1.39.0, Redis 8.0, RabbitMQ 4.0, Nacos 3.2.2
- **前端应用**: Vue3 + UniApp (uview-plus) 客户端（居民端）、Vue3 管理端（社区管理后台）
- **服务组成**: `gateway`（网关）、`user-service`（用户与社区）、`memorial-service`（纪念祭扫，规划中）、`notify-service`（通知，规划中）

#### 安装教程

1.  启动中间件：`docker compose up -d`（PostgreSQL、Redis、Nacos、RabbitMQ）
2.  启动 Nacos 配置：将各服务 `application.yml` 对应配置发布至 Nacos 配置中心
3.  依次启动 `gateway`、`user-service` 等微服务模块

#### 使用说明

1.  客户端（UniApp）通过网关 `http://122.51.234.113:10000` 访问后端接口
2.  管理端（Vue3）用于社区管理员审核逝者档案、管理祭扫预约与发布文明祭扫公告
3.  完整文档体系见 [项目文档](./项目文档/README.md)

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
