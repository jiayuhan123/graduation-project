# Notification Service 通用通知服务

`notification-service`（默认端口 8003）提供站内通知查询和已读状态，并消费 `user.registered` 事件异步创建欢迎通知。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/notifications?page=1&size=10&read=false` | 分页查询当前用户通知 |
| GET | `/api/notifications/unread-count` | 未读数量 |
| PUT | `/api/notifications/{id}/read` | 标记一条已读 |
| PUT | `/api/notifications/read-all` | 全部标记已读 |

服务使用独立 `notification_db`。`source_event_id` 唯一约束保证重复注册事件不会生成多条通知。消费最多尝试 4 次，失败后进入 `notification.dead-letter.v1`，运维处理前不得直接丢弃死信。

运行依赖 PostgreSQL、Nacos 和 RabbitMQ，配置沿用 `DB_*`、`NACOS_*`、`RABBITMQ_*` 环境变量。
