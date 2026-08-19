# Audit Service 通用审计服务

`audit-service`（默认端口 8004）异步消费用户注册和用户管理/RBAC 操作事件，将不含敏感信息的记录写入独立 `audit_db`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/audits/me?page=1&size=10&action=...` | 查询当前用户操作记录 |

服务消费 `audit.user-registered.v1` 与 `audit.operation.v1`，以 `event_id` 唯一约束保证幂等。重试耗尽后消息进入 `audit.dead-letter.v1`。

当前仅开放个人审计查询。全局审计管理接口必须在选题确定后增加专用权限码、查询范围限制和脱敏策略，不能直接复用个人接口扩权。
