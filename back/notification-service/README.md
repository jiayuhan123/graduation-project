# Notification Service 通用通知服务

`notification-service` 是单体内的通知领域模块，不单独启动或占用端口。它提供站内通知查询和已读状态，并消费 `user.registered` 事件异步创建欢迎通知，数据统一写入 `monolith_db`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/notifications?page=1&size=10&read=false` | 分页查询当前用户通知 |
| GET | `/api/notifications/unread-count` | 未读数量 |
| PUT | `/api/notifications/{id}/read` | 标记一条已读 |
| PUT | `/api/notifications/read-all` | 全部标记已读 |

通知数据位于 `monolith_db.notification`。`source_event_id` 唯一约束保证重复注册事件不会生成多条通知。消费最多尝试 4 次，失败后进入 `notification.dead-letter.v1`，运维处理前不得直接丢弃死信。

运行依赖服务器 PostgreSQL、Redis 和 RabbitMQ，连接由 `monolith-app` 统一配置。
