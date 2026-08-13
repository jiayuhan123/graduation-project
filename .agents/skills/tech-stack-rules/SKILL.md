---
name: tech-stack-rules
description: >-
  提供了本项目使用的最新技术栈（如 Spring Boot 4、Java 21、Spring Cloud 2025、Nacos 等）的严格语法与版本规范。AI 在编写、重构或调试 Java/Spring 代码前必须参考此规范，以避免使用过时语法（如 javax 等）导致报错。
---

# 技术栈与语法规范 (Technology Stack & Syntax Rules)

## 概述
本项目的技术栈非常激进，使用了当前最新甚至处于前沿版本的框架和语言特性。
为了避免生成过时的代码导致系统编译失败或运行报错，AI 助手在编写代码、提供重构建议时，**必须**严格遵循以下版本特性和语法规则。

## 核心技术栈版本
- **Java**: `21`
- **Spring Boot**: `4.0.x` (当前父 POM 配置为 `4.0.6`)
- **Spring Cloud**: `2025.1.1`
- **Spring Cloud Alibaba (含 Nacos)**: `2025.1.0.0`
- **MyBatis-Plus**: `3.5.15` (专属支持 Spring Boot 4 的 `mybatis-plus-spring-boot4-starter`)
- **数据库**: `PostgreSQL 18` (全面替代 MySQL)
- **Sa-Token**: `1.39.0`

---

## 编码规则与语法要求 (强制执行)

### 1. 彻底拥抱 Jakarta EE (极度重要)
- **绝不**允许在任何新代码中使用 `javax.*` 包！
- 所有 Servlet、验证、持久化注解必须导入 **`jakarta.*`**。
  - 例如：`jakarta.servlet.http.HttpServletRequest`
  - 例如：`jakarta.validation.constraints.NotBlank`
  - 例如：`jakarta.annotation.Resource`

### 2. Java 21 现代语法规范
- **Record 类**：对于纯数据载体（DTO/VO/Request/Response），优先推荐使用 Java Record 替代 Lombok 的 `@Data` 类。
- **模式匹配**：优先使用 Pattern Matching for `instanceof` 和新型 `switch` 表达式。
- **文本块**：在编写较长的 SQL、JSON 或提示字符串时，必须使用多行文本块（`"""`）。
- **虚拟线程**：Spring Boot 4 默认深度集成虚拟线程。在涉及并发处理时，应优先考虑虚拟线程生态下的写法。

### 3. Spring Boot 4.x & Spring Cloud 2025 规范
- **配置加载**：使用 `spring.config.import` 方式引入 Nacos 等外部配置，不要使用早已淘汰的 `bootstrap.yml`。
- **服务调用与负载均衡**：Netflix Ribbon 及其相关注解已完全废弃。服务调用依赖 `OpenFeign`，负载均衡严格使用 `Spring Cloud LoadBalancer`。
- **废弃组件**：绝不使用 `WebSecurityConfigurerAdapter`（若有安全配置，需使用基于 `SecurityFilterChain` 的组件配置方式）。

### 4. MyBatis-Plus 3.5.x 规范
- 使用最新的配置方式：配置分页、乐观锁等插件时，必须使用 `MybatisPlusInterceptor` 并添加相应的 InnerInterceptor（如 `PaginationInnerInterceptor`），不要使用旧版的 `PaginationInterceptor`。
- 实体类主键生成策略等注解请严格使用新版规范（如 `@TableId(type = IdType.ASSIGN_ID)` 等）。

### 5. Sa-Token 鉴权规范
- 本项目抛弃了繁重的 Spring Security/Shiro，统一使用 Sa-Token 进行权限和会话管理。
- 路由拦截或 AOP 鉴权请使用 `StpUtil` 提供的 API，并配合全局异常处理器捕获 `SaTokenException` 的子类进行统一 JSON 响应。

### 6. 基础设施与部署规范 (Docker)
- 服务器端安装的是新版 Docker，集成了 Compose 插件。
- 在提供任何容器编排相关的命令时，**必须使用 `docker compose`**（有空格），**严禁使用已被淘汰的旧版 `docker-compose`**（带横杠）。

---

## 触发条件
当被要求：
1. "写一个接口"
2. "生成 CRUD 代码"
3. "解决 Spring/Java 报错"
4. "分析配置"
时，AI 助手在思考过程中应主动结合此 `tech-stack-rules` 规范进行校验。
