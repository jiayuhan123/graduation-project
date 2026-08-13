# Design and Implementation of a Civilized Memorial Service System for Specific Communities

#### Description
This is the graduation project "Design and Implementation of a Civilized Memorial Service System for Specific Communities". Built on a microservice architecture, the system provides online and offline civilized memorial services for specific communities (residential areas, streets, etc.), covering deceased memorial archives management, online memorial activities (flowers, candles, messages), off-peak memorial appointment scheduling, and community announcements, promoting a green, civilized, and safe memorial culture.

#### Software Architecture
- **Core Framework**: Java 21, Spring Boot 4.0.6, Spring Cloud 2025.1.1, Spring Cloud Alibaba 2025.1.0.0
- **Data Layer**: PostgreSQL 18, MyBatis-Plus 3.5.15
- **Auth & Middleware**: Sa-Token 1.39.0, Redis 8.0, RabbitMQ 4.0, Nacos 3.2.2
- **Frontend**: Vue3 + UniApp (uview-plus) client app (residents), Vue3 management console (community admins)
- **Services**: `gateway`, `user-service` (users & communities), `memorial-service` (memorial core, planned), `notify-service` (planned)

#### Installation

1.  Start middleware: `docker compose up -d` (PostgreSQL, Redis, Nacos, RabbitMQ)
2.  Publish service configurations to the Nacos config center
3.  Start microservices: `gateway`, `user-service`, etc.

#### Instructions

1.  The UniApp client accesses backend APIs via the gateway `http://122.51.234.113:10000`
2.  The Vue3 management console is used by community admins to audit memorial archives, manage appointments, and publish announcements
3.  Full documentation: see [项目文档](./项目文档/README.md)

#### Contribution

1.  Fork the repository
2.  Create Feat_xxx branch
3.  Commit your code
4.  Create Pull Request


#### Gitee Feature

1.  You can use Readme\_XXX.md to support different languages, such as Readme\_en.md, Readme\_zh.md
2.  Gitee blog [blog.gitee.com](https://blog.gitee.com)
3.  Explore open source project [https://gitee.com/explore](https://gitee.com/explore)
4.  The most valuable open source project [GVP](https://gitee.com/gvp)
5.  The manual of Gitee [https://gitee.com/help](https://gitee.com/help)
6.  The most popular members  [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
