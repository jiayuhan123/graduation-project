# Content 内容题库领域

`content` 是内容题库领域的 Maven 聚合模块，本身不运行，也不注册到 Nacos。

```text
content
├─ content-api      跨服务 HTTP 契约、DTO 与稳定路径常量
└─ content-service  内容题库服务，默认端口 8005，使用 content_db
```

当前已提供：

- 5 张内容业务表及 1 张事务发件箱表对应的 Entity、Mapper 和复合键模型；
- PostgreSQL `JSONB` 题目字段映射；
- 学习端已启用内容模块、知识点的安全查询；
- 管理端内容模块、知识点、题目和错因字典的创建、查询、修改、状态切换与软删除；
- 题目 `DRAFT → ENABLED / DISABLED` 审核流，编辑已发布题目后自动退回草稿；
- 管理接口通过用户服务实时校验 `content:manage` 权限；
- 乐观锁、稳定编码唯一性、层级与引用关系校验；
- 内容变更与审计事件同事务写入，定时可靠投递至审计服务；
- 受共享令牌保护的题目批量查询、题目详情和知识点内部接口；
- `ContentFeignClient` 及稳定的 record 请求/响应契约；
- 权限守卫、事务发件箱状态转换及数据库映射测试。

学生接口不会返回题库、标准答案、解析、题目生成规则或错因管理字典；这些能力只能经管理接口或受共享令牌保护的内部接口访问。

如果 `content_db` 已经创建过原有 5 张业务表，需要手工执行一次幂等升级脚本：

```powershell
psql -d content_db -f content-service/src/main/resources/db/upgrade-outbox.sql
```

脚本只新增 `content_outbox_event`，不添加物理外键，也不会导入业务内容数据。

```powershell
.\mvnw.cmd -pl content/content-service -am test
```
