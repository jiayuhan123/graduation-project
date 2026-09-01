# User API 用户服务调用契约

`user-api` 是用户服务与调用方共享的内部 HTTP 契约模块，不保存业务状态，也不注册为 Nacos 服务。

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

调用方只依赖本模块，不依赖 `user-service` 的 Controller、Service、Entity 或 Mapper。`UserFeignClient` 使用 Nacos 中的 `user-service` 服务名，并由 Spring Cloud LoadBalancer 选择实例。

## 调用方接入

在调用方启动类启用指定客户端：

```java
@EnableFeignClients(clients = UserFeignClient.class)
@SpringBootApplication
public class LearningServiceApplication {
}
```

配置与用户服务相同的内部 Token：

```yaml
app:
  internal-api:
    token: ${INTERNAL_API_TOKEN:}
```

调用时直接注入客户端：

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

`UserFeignConfiguration` 会统一写入 `X-Internal-Token`；未配置 Token 时会在发起请求前明确报错。内部调用不经过 Gateway。
