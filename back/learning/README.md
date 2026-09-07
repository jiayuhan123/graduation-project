# Learning 学习闭环领域

`learning` 是学习闭环领域的 Maven 聚合模块，本身不运行，也不注册到 Nacos。

```text
learning
├─ learning-api      跨服务 HTTP 契约、DTO 与稳定路径常量
└─ learning-service  学习闭环服务，默认端口 8006，使用 learning_db
```

当前已提供：

- 11 张学习库表对应的 Entity、Mapper 和复合键模型；
- 基于已发布固定题库的自主练习组题，优先安排未练题和未解决错题；
- 教师作业按学生层级生成唯一练习任务，并将开始、得分和完成情况回写教学服务；
- 自主练习逐题即时判分和重复作答，教师作业最终提交后统一反馈；
- 整数、小数、分数、百分数、判断题答案归一化比较；
- 错题记录、掌握度更新、变化轨迹、复习时间和个性化巩固推荐闭环；
- 学生本人报告，以及受权限和真实师生关系约束的教师/管理员学情查询；
- 练习任务、答题记录、错题、掌握度及变化、推荐、拍照批改任务的本人查询接口；
- 练习任务内部查询契约与 `LearningFeignClient`；
- 所有学生查询的 `student_id` 归属约束，以及数据库映射覆盖测试。

OCR 处理和人工复核仍属于后续命令用例；当前没有用空返回或伪实现代替未接入的识别能力。

已有学习库需要先执行一次幂等升级：

```powershell
psql -v ON_ERROR_STOP=1 -d learning_db -f learning-service/src/main/resources/db/upgrade-online-closure.sql
```

```powershell
.\mvnw.cmd -pl learning -am test
```
