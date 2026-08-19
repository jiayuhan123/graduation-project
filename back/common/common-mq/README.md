# common-mq

RabbitMQ 领域事件公共契约与可靠发布能力。

## 公共资源

| 名称 | 类型/用途 |
| --- | --- |
| `app.domain.events` | 持久化 Topic Exchange |
| `app.dead-letter` | 持久化 Direct Dead Letter Exchange |
| `user.registered` | 用户注册事件路由键 |
| `audit.operation` | 通用审计事件路由键 |

模块使用 Spring AMQP 4 的 Jackson 3 消息转换器。`DomainEventPublisher` 以 mandatory 发布并等待 Publisher Confirm；不可路由、Nack 或超时均抛出发布异常。

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
app:
  mq:
    confirm-timeout: 5s
```

公共模块只拥有交换机和事件 DTO。消费者必须在自己的服务中声明队列、绑定、重试和死信队列，并以 `eventId` 唯一约束保证至少一次投递下的幂等性。事件字段只允许包含跨服务所需信息，不得携带密码、Token 或 AccessKey。
