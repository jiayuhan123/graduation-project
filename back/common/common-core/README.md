# common-core

`common-core` 是后端最基础的共享组件，提供与具体 Web 技术栈无关的响应和异常模型。网关与所有业务服务均可引用。

## 核心能力

| 类型 | 位置 | 用途 |
| --- | --- | --- |
| `Result<T>` | `result/Result.java` | 统一接口响应结构和成功/失败工厂方法 |
| `ResultCode` | `exception/ResultCode.java` | 集中定义通用及各业务域错误码 |
| `BusinessException` | `exception/BusinessException.java` | 表示可预期、可直接反馈给客户端的业务异常 |

## 统一响应

响应包含三个字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `Integer` | 业务状态码，`200` 表示成功 |
| `message` | `String` | 面向调用方的结果说明 |
| `data` | 泛型 | 业务数据，无数据时为 `null` |

使用示例：

```java
return Result.success(data);
return Result.success("创建成功", data);
return Result.error(ResultCode.BAD_REQUEST);
```

## 错误码分区

| 范围 | 含义 |
| --- | --- |
| `200` | 操作成功 |
| `4xx` | 参数、认证、权限、资源等通用客户端错误 |
| `5xx` | 服务端或服务可用性错误 |
| `1xxx` | 用户模块业务错误 |
| `2xxx` | 文件模块业务错误（预留） |

新增错误码时，应优先扩展 `ResultCode`，并保持同一业务域使用稳定、互不冲突的编号区间。

## 业务异常

业务层遇到预期内的失败时抛出 `BusinessException`：

```java
if (usernameExists) {
    throw new BusinessException(ResultCode.USERNAME_EXISTS);
}
```

Servlet 服务中的 `common-web` 会将该异常转换为 `Result`；网关则使用自己的响应式异常处理器。

## 依赖边界

本模块仅依赖 Lombok 和 Jackson 注解，不依赖 Spring MVC、WebFlux、数据库或 Redis。不要在这里加入只服务于某个业务模块的 DTO、实体或工具类。
