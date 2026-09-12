# User API 用户服务调用契约

`user-api` 是用户领域与其他模块共享的调用契约，不保存业务状态，也不独立运行。

## 包结构

```text
xyz.jiayuhan.userapi
├─ UserApiConstants.java
├─ client
│  ├─ UserFeignClient.java
│  └─ UserFeignConfiguration.java
└─ model
   ├─ request
   │  └─ BatchUserQueryRequest.java
   └─ response
      ├─ UserSummaryResponse.java
      └─ UserValidationResponse.java
```

调用方只依赖本模块，不依赖 `user-service` 的 Controller、Service、Entity 或 Mapper。接口名和 Feign 注解为答辩后重新拆分保留；当前单体不启用 `@EnableFeignClients`，由 `monolith-app` 中的 `LocalUserClient` 直接调用用户领域服务。

## 当前单体接入

单体入口集中注册本地适配器，业务模块仍按契约注入：

```java
@Service
@RequiredArgsConstructor
public class StudentReferenceService {

    private final UserFeignClient userFeignClient;

    public Result<UserValidationResponse> validateStudent(Long userId) {
        return userFeignClient.validateUser(userId, "student");
    }
}
```

当前调用不经过 HTTP、Gateway、Nacos 或负载均衡。`UserFeignConfiguration` 仅作为未来重新拆分时的兼容代码保留，不参与单体运行。
