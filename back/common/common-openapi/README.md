# common-openapi

为 Servlet 服务自动创建 OpenAPI 元数据。各服务只需要配置：

```yaml
app:
  openapi:
    title: 示例服务 API
    description: 服务接口说明
    version: 1.0.0
```

文档默认由 springdoc 提供 `/v3/api-docs` 和 `/swagger-ui.html`，并由 Gateway/Knife4j按服务发现聚合。生产环境是否公开文档端点应按安全策略决定。
