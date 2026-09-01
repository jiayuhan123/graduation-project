# User 用户领域

`user` 是用户领域的 Maven 聚合目录，与 `common` 聚合目录采用相同结构，本身不运行也不注册到 Nacos。

```text
user
├─ pom.xml
├─ user-api
│  ├─ UserFeignClient
│  └─ 跨服务 Request/Response
└─ user-service
   ├─ Controller、Service、Mapper、Entity
   └─ 用户服务运行配置
```

- [`user-api`](./user-api/README.md)：供其他业务服务依赖的内部调用契约。
- [`user-service`](./user-service/README.md)：端口 `8001` 的用户与身份服务实现。

构建用户领域：

```powershell
.\mvnw.cmd -pl user -am test
```

只构建可运行用户服务及其依赖：

```powershell
.\mvnw.cmd -pl user-service -am test
```
