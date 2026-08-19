---
name: project-dependency-versions
description: >-
  记录了本项目后端（Java/Spring）与前端（UniApp）所有核心依赖库和插件的精准版本号清单。AI 在向项目中引入新依赖、处理包冲突、排查兼容性问题时，必须严格参考此版本清单。
---

# 项目核心依赖版本清单 (Project Dependency Versions)

## 概述
为了保证系统高频更新下的版本兼容性与稳定性，本技能罗列了项目当前使用的所有具体插件和依赖库的大版本与小版本。AI 助手在进行依赖管理（如修改 `pom.xml` 或 `package.json`）时，**绝不**随意猜测或使用不兼容的版本，必须严格对照下表。

---

## ☕ 后端依赖 (Backend / Java)

### 1. 基础环境与构建插件
- **Java SDK**: `21`
- **Maven Compiler Plugin**: 目标与源版本均配置为 `21`
- **Spring Boot Maven Plugin**: 版本与 Spring Boot 父工程一致

### 2. Spring 生态核心框架 (极其激进)
- **Spring Boot**: `4.0.6` (所有微服务模块的 Parent 版本)
  - *注*: 个别基础或第三方依赖可能强依赖 `spring-boot-starter-web: 3.2.0`，需注意冲突排除。
- **Spring Cloud**: `2025.1.1`
  - *包含*: `spring-cloud-starter-loadbalancer`, `spring-cloud-starter-openfeign` 等。
- **Spring Cloud Alibaba**: `2025.1.0.0`
  - *包含*: `spring-cloud-starter-alibaba-nacos-discovery`, `spring-cloud-starter-alibaba-nacos-config`。

### 3. 数据持久层与对象存储
- **MyBatis-Plus**: 
  - `3.5.15` (主微服务使用 `mybatis-plus-spring-boot4-starter`)
  - `3.5.12` (部分通用模块兼容使用 `mybatis-plus-spring-boot3-starter`)
- **阿里云 OSS Java SDK V2**: `com.aliyun:alibabacloud-oss-v2:0.5.1`
- **PostgreSQL Driver**: `org.postgresql:postgresql` (版本由 Spring Boot 4.0.6 的 BOM 自动管控)

### 4. 权限、安全与网关组件
- **Sa-Token 核心**: `1.39.0` (`sa-token-spring-boot3-starter`)
- **Sa-Token 整合 Redis**: `1.39.0` (`sa-token-redis-jackson`)
- **Sa-Token 网关整合**: `1.39.0` (`sa-token-reactor-spring-boot3-starter`)
- **Knife4j 网关 Swagger**: `4.5.0` (`knife4j-gateway-spring-boot-starter`)

### 5. 其他辅助依赖库
- **Lombok**: `1.18.30` (明确指定了版本)
- **Jackson-annotations**: `2.15.2` (明确指定了版本)
- **Commons-pool2**: 用于 Redis 连接池支持，受 Spring Boot BOM 控制。
- **Jackson-datatype-jsr310**: 用于 Java 8+ 时间处理。

---

## 📱 前端依赖 (Frontend / client-app)

### 1. 核心框架
- **UniApp / Vue**: 通过脚手架搭建的默认版本机制。

### 2. UI 框架与工具库
- **uview-plus**: `^3.3.36` (用于 Vue3 + UniApp 的核心 UI 组件库)
- **dayjs**: `^1.11.10` (时间处理库)
- **clipboard**: `^2.0.11` (剪贴板处理)

---

## 触发条件与使用要求
当用户要求：
1. "帮我加一个引入 Redis/阿里云 OSS 的依赖"
2. "我的项目好像有 Jar 包版本冲突"
3. "帮我写前端/后端的 `pom.xml` 或 `package.json`"
4. "增加某个第三方 SDK"

**AI 响应要求：**
- 立即查询本表。
- 对于后端依赖，如果 Spring Boot / Cloud 提供了官方统一的 BOM，**一定不要写死 `<version>`**。
- 引入不在表内的第三方插件时，必须检查该插件能否兼容 **Spring Boot 4 / Java 21 / Jakarta EE**。
