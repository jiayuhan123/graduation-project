# Common 公共能力层

`common` 是公共组件的 Maven 聚合模块，用于沉淀多个微服务共享的基础能力。它的打包类型为 `pom`，没有启动类，也不会作为服务注册到 Nacos。

## 子模块

| 子模块 | 提供能力 | 适用范围 | 文档 |
| --- | --- | --- | --- |
| `common-core` | 统一响应、错误码、业务异常 | 网关和所有业务服务 | [查看文档](./common-core/README.md) |
| `common-web` | Spring MVC 自动配置、用户上下文、统一异常处理 | Servlet 业务服务 | [查看文档](./common-web/README.md) |
| `common-redis` | RedisTemplate 序列化、Sa-Token Redis 支持 | 需要 Redis 的 Servlet 业务服务 | [查看文档](./common-redis/README.md) |

## 依赖关系

```text
common-core
   ├── common-web
   └── common-redis
```

`common-web` 与 `common-redis` 都依赖 `common-core`，两者互不依赖。

## 选择规则

- 所有模块都可以引用 `common-core`。
- Spring MVC 业务服务可以引用 `common-web`。
- 需要 Redis 或 Sa-Token Servlet 能力的业务服务可以引用 `common-redis`。
- Spring Cloud Gateway 使用 WebFlux/Reactor，不能引用 `common-web` 或 `common-redis`，以免混入 Servlet 技术栈；网关应按自身响应式模型单独配置 Redis 和异常处理。

## 在业务服务中引用

版本已由后端父工程统一管理，子模块无需声明版本号：

```xml
<dependency>
    <groupId>xyz.jiayuhan</groupId>
    <artifactId>common-core</artifactId>
</dependency>
<dependency>
    <groupId>xyz.jiayuhan</groupId>
    <artifactId>common-web</artifactId>
</dependency>
<dependency>
    <groupId>xyz.jiayuhan</groupId>
    <artifactId>common-redis</artifactId>
</dependency>
```

只引入服务实际需要的组件，避免公共模块逐渐演变成包含所有依赖的“大而全”工具包。

## 构建验证

在 `back` 目录执行：

```powershell
.\mvnw.cmd -pl common -am verify
```

公共组件没有独立运行命令。完成构建后，它们会作为普通 Jar 被业务服务加载。
