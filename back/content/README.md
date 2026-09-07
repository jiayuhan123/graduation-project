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

### 一年级人教版种子数据

首次准备一年级人教版基础计算内容时，在已经执行 `schema.sql` 的 `content_db` 中手工运行：

```powershell
psql -v ON_ERROR_STOP=1 -d content_db -f content-service/src/main/resources/db/seed-grade1-pep.sql
```

该脚本会导入 9 个内容模块、65 个知识点、14 个基础错因、27 道联调固定题及对应的主知识点绑定。脚本按稳定编码判重，可以重复执行；当前不会导入模板题、图形、数学游戏、综合实践和复习单元。

### 二年级人教版种子数据

完成一年级种子数据导入后，在同一个 `content_db` 中手工运行：

```powershell
psql -v ON_ERROR_STOP=1 -d content_db -f content-service/src/main/resources/db/seed-grade2-pep.sql
```

该脚本会导入二年级上下册 7 个基础计算内容模块、61 个知识点、10 个二年级专项错因、38 道联调固定题及对应的主知识点绑定。脚本按稳定编码判重，可以重复执行；当前范围不包含分类、长度、时间实践、数学游戏、综合实践和复习单元。一年级脚本中的通用错因会由二年级内容继续复用，因此推荐按年级顺序导入。

### 三至六年级人教版种子数据

继续按年级顺序运行：

```powershell
psql -v ON_ERROR_STOP=1 -d content_db -f content-service/src/main/resources/db/seed-grade3-pep.sql
psql -v ON_ERROR_STOP=1 -d content_db -f content-service/src/main/resources/db/seed-grade4-pep.sql
psql -v ON_ERROR_STOP=1 -d content_db -f content-service/src/main/resources/db/seed-grade5-pep.sql
psql -v ON_ERROR_STOP=1 -d content_db -f content-service/src/main/resources/db/seed-grade6-pep.sql
```

| 年级 | 计算模块 | 知识点 | 新增专项错因 | 固定题 |
| --- | ---: | ---: | ---: | ---: |
| 三年级 | 5 | 34 | 8 | 20 |
| 四年级 | 6 | 44 | 9 | 24 |
| 五年级 | 6 | 41 | 10 | 24 |
| 六年级 | 6 | 39 | 10 | 24 |

上述脚本只保留整数、小数、分数、四则运算、运算律、方程、比、比例和百分数等计算主线。观察物体、图形与测量、位置、统计、概率、数学广角、综合实践及复习单元不导入。所有脚本按稳定编码判重，可以安全重复执行。

```powershell
.\mvnw.cmd -pl content/content-service -am test
```
