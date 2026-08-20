# common-feign

为 Servlet 业务服务提供统一的 Spring Cloud OpenFeign 与 Spring Cloud LoadBalancer 基础能力。本模块不包含任何业务接口或业务 DTO，Gateway 是 WebFlux，不得引用本模块。

## 自动配置能力

- 从 `UserContext` 重建 `X-User-ID`，不转发调用方手工写入的身份头。
- 从 MDC 传递 `X-Trace-ID`，保持同步调用链路一致。
- 将下游非 2xx HTTP 响应转换为 `RemoteServiceException`，对外统一为 `SERVICE_UNAVAILABLE`。
- 显式引入 Spring Cloud LoadBalancer，通过 Nacos 中的服务名选择实例。

## 使用方式

调用方服务按需引入：

```xml
<dependency>
    <groupId>xyz.jiayuhan</groupId>
    <artifactId>common-feign</artifactId>
</dependency>
```

在调用方应用启用并明确列出客户端：

```java
@EnableFeignClients(clients = UserFeignClient.class)
@SpringBootApplication
public class ExampleServiceApplication {
}
```

```java
@FeignClient(name = "user-service", path = "/internal/users")
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<UserSummaryVO> getById(@PathVariable Long id);
}
```

业务客户端与其最小契约应由提供方拥有，例如单独建立 `user-api`；不得把不同服务的 DTO 堆入 `common-feign`。

## 超时与日志

使用 Spring Cloud OpenFeign 原生配置，不额外发明重复的配置前缀：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 2000
            readTimeout: 5000
            loggerLevel: basic
```

OpenFeign 默认不自动重试。写操作没有幂等保护时，禁止配置自动重试。本轮不引入熔断与降级，后续由独立的韧性模块统一提供。
