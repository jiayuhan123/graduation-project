# common-openapi

为模块化单体自动创建 OpenAPI 元数据。入口应用只需要配置：

```yaml
app:
  openapi:
    title: 示例服务 API
    description: 服务接口说明
    version: 1.0.0
```

文档由 `monolith-app` 中的 springdoc 直接提供 `/v3/api-docs` 和 `/swagger-ui.html`，不经过 Gateway 或服务发现。生产环境是否公开文档端点应按安全策略决定。
