# common-web

`common-web` 为 Spring MVC（Servlet）业务服务提供自动装配的通用 Web 能力。模块被服务引用后，无需额外添加组件扫描或手动注册配置。

## 自动装配内容

自动配置入口位于：

```text
src/main/resources/META-INF/spring/
└── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

当前装配两个组件：

| 组件 | 能力 |
| --- | --- |
| `WebMvcConfig` | 为所有路径注册 `UserInfoInterceptor` |
| `GlobalExceptionHandler` | 将业务异常、参数校验异常和未知异常转换为统一响应 |

`WebMvcConfig` 只在 Servlet Web 应用中生效。

## 用户身份上下文

请求经过网关鉴权后，网关会注入 `X-User-ID` 请求头。`UserInfoInterceptor` 将它解析为 `Long` 并保存到当前线程的 `UserContext`：

```java
Long userId = UserContext.getUserId();
```

请求完成后，拦截器会清理 `ThreadLocal`，避免线程复用导致用户信息串读。

注意事项：

- 此拦截器只提取身份，不负责鉴权；没有请求头时同样会放行。
- 业务代码必须处理 `UserContext.getUserId()` 可能返回 `null` 的情况。
- 服务部署时应限制外部绕过网关直接访问，防止伪造 `X-User-ID`。

## 异常处理

| 异常 | 业务码 | 响应说明 |
| --- | ---: | --- |
| `BusinessException` | 异常携带的错误码 | 返回可预期的业务错误 |
| `MethodArgumentNotValidException` | `400` | 汇总 `@Valid` 请求体字段错误 |
| `ConstraintViolationException` | `400` | 汇总单参数或表单校验错误 |
| 其他 `Exception` | `500` | 记录完整日志，对外返回通用错误 |

当前处理器返回的 HTTP 状态码通常为 `200`，调用方需要读取响应体中的 `code`。

## 引用方式

```xml
<dependency>
    <groupId>xyz.jiayuhan</groupId>
    <artifactId>common-web</artifactId>
</dependency>
```

本模块已经传递引入 `common-core`、Spring MVC 和 Jakarta Validation。

## 使用边界

不要在 Spring Cloud Gateway 中引用本模块。Gateway 基于 WebFlux/Reactor，与这里的 Servlet API、`HandlerInterceptor` 和 `ThreadLocal` 请求模型不同。
