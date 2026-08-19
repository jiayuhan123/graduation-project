# common-security

在 Servlet 服务中解析网关重建的 `X-User-ID`，并通过 `UserContext` 暴露当前用户。

```java
Long userId = UserContext.requireUserId();
```

`requireUserId()` 在缺少身份时抛出 401；`getUserId()` 可用于允许匿名访问的场景。拦截器在请求结束后清理 ThreadLocal。该模块只负责内部身份传递，不验证 Sa-Token；生产部署必须禁止公网绕过网关直连服务。
