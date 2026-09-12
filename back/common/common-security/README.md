# common-security

在模块化单体中提供 `UserContext` 和受共享令牌保护的 `/internal/**` 接口支持。公网 `/api/**` 的登录态由 `monolith-app` 直接通过 Sa-Token 校验，不读取或信任客户端提交的 `X-User-ID`。

```java
Long userId = UserContext.requireUserId();
```

`requireUserId()` 在缺少身份时抛出 401；`getUserId()` 可用于允许匿名访问的场景。`monolith-app` 的认证拦截器负责校验 Sa-Token，并在请求结束后清理 ThreadLocal；生产部署只需对外暴露单体业务入口，不公开 `/internal/**`。
