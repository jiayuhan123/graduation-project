# Learning 学习闭环领域

`learning` 是学习闭环领域的 Maven 聚合模块，本身不运行，也不注册到 Nacos。

```text
learning
├─ learning-api      跨服务 HTTP 契约、DTO 与稳定路径常量
└─ learning-service  学习闭环服务，默认端口 8006，使用 learning_db
```

当前已提供：

- 11 张学习库表对应的 Entity、Mapper 和复合键模型；
- 练习任务、答题记录、错题、掌握度及变化、推荐、拍照批改任务的本人查询接口；
- 练习任务内部查询契约与 `LearningFeignClient`；
- 所有学生查询的 `student_id` 归属约束，以及数据库映射覆盖测试。

练习生成、答案提交与可信批改、掌握度更新、推荐生成、OCR 处理和人工复核属于后续命令用例；当前没有用空返回或伪实现代替这些核心算法。

```powershell
.\mvnw.cmd -pl learning -am test
```
