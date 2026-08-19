# common-data

MyBatis-Plus 公共数据层自动配置，统一启用 PostgreSQL 分页、乐观锁和实体审计时间填充。

```yaml
app:
  data:
    max-page-size: 100
    overflow: false
```

分页最大返回 100 条；超过总页数默认返回空页。实体字段只有标注 `@TableField(fill = ...)` 才会自动填充 `createdAt` / `updatedAt`。数据库表结构仍由各服务自己的 Flyway 管理。
