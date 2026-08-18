# common-redis

`common-redis` 为 Servlet 业务服务提供 Redis 与 Sa-Token 的共享依赖，并自动创建统一序列化策略的 `RedisTemplate<String, Object>`。

## 核心能力

- 引入 Spring Data Redis 和 Lettuce 连接池支持；
- 引入 Sa-Token Servlet Starter 及 Redis 持久化适配；
- 自动装配 `RedisTemplate<String, Object>`；
- 支持 `LocalDateTime` 等 Java 时间类型的 JSON 序列化。

## 序列化策略

| Redis 数据部分 | 序列化器 |
| --- | --- |
| Key / Hash Key | `StringRedisSerializer` |
| Value / Hash Value | `GenericJacksonJsonRedisSerializer` |

业务服务可直接注入：

```java
private final RedisTemplate<String, Object> redisTemplate;
```

Value 使用带类型信息的 JSON 序列化。不要在同一批 Key 上混用其他序列化策略，否则可能出现历史数据无法反序列化的问题。

## 引用方式

```xml
<dependency>
    <groupId>xyz.jiayuhan</groupId>
    <artifactId>common-redis</artifactId>
</dependency>
```

服务还需要在 `application.yml`、环境变量或配置中心提供 Redis 连接信息：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
```

## 自动装配

`RedisConfig` 通过 Spring Boot 的 `AutoConfiguration.imports` 加载。若业务服务定义了同名 `redisTemplate` Bean，当前配置不会自动退让，因此应避免重复声明同名 Bean，或先为自动配置增加明确的条件注解。

## 使用边界

- 本模块面向 Servlet 业务服务。
- Gateway 基于 WebFlux，不应引用该模块；网关在自身模块中单独引入响应式 Sa-Token 与 Redis 依赖。
- Redis 凭据不应提交到仓库，生产环境应从环境变量或 Nacos 注入。
