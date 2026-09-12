# Learning 学习闭环领域

`learning` 是模块化单体中的学习闭环领域，本身不独立运行。API 契约用于保持模块边界，当前由 `monolith-app` 的进程内适配器实现；所有表统一位于 `monolith_db`。下文旧独立库升级命令仅保留作迁移历史，不用于当前开发。

```text
learning
├─ learning-api      模块契约、DTO 与稳定路径常量
└─ learning-service  学习闭环实现，由 monolith-app 统一装配
```

当前已提供：

- 11 张学习业务表及 1 张作业进度同步发件箱表对应的 Entity、Mapper 和复合键模型；
- 基于已发布固定题库的自主练习组题，优先安排未练题和未解决错题；
- 教师作业按学生层级生成唯一练习任务，并通过本地事务发件箱有序、可重试地回写开始、得分和完成情况；
- 自主练习逐题即时判分和重复作答，教师作业最终提交后统一反馈；
- 整数、小数、分数、百分数、判断题答案归一化比较；
- 错题记录、掌握度更新、变化轨迹、复习时间和个性化巩固推荐闭环；
- 学生本人报告，以及受权限和真实师生关系约束的教师/管理员学情查询；
- 练习任务、答题记录、错题、掌握度及变化、推荐、拍照批改任务的本人查询接口；
- 练习任务内部查询契约与 `LearningFeignClient`；
- 任务行锁、掌握度事务级 advisory lock、作业任务唯一约束和推荐状态条件更新，避免重复提交与并发更新覆盖；
- 所有学生查询的 `student_id` 归属约束，以及核心流程、远程失败重试和数据库映射测试。

业务边界固定为人教版小学数学计算类知识点，只从数据库中选择已经审核启用的固定题目，不调用大模型生成题目。OCR 处理和人工复核本轮不开发；查询接口只用于读取已有数据，前端明确显示“暂未开放”，没有用空返回或伪实现代替识别能力。

## 接口说明

所有外部接口统一以 `/api/learning` 为前缀，雪花 ID 在前端按字符串传递。

| 方法与路径 | 调用者 | 说明 |
| --- | --- | --- |
| `POST /practice-tasks` | 学生 | 从固定题库创建自主练习 |
| `POST /assignment-tasks/{assignmentId}` | 学生 | 幂等创建或获取本人的作业练习 |
| `GET /practice-tasks[/{taskId}]` | 学生 | 查询本人的任务列表或详情 |
| `POST /practice-tasks/{taskId}/start` | 学生 | 开始练习；作业进度异步可靠回写 |
| `POST /practice-tasks/{taskId}/questions/{taskQuestionId}/answers` | 学生 | 自主练习立即判题并允许重试；教师作业只保存答案 |
| `POST /practice-tasks/{taskId}/complete` | 学生 | 提交整份任务；教师作业在此统一判题且此后不可修改 |
| `GET /overview`、`GET /reports/me` | 学生 | 查询本人概览和完整学习报告 |
| `GET /reports/students/{studentId}` | 教师、管理员 | 教师仅限本人班级学生；管理员可查询全部学生 |
| `GET /answers`、`GET /errors`、`GET /mastery` | 学生 | 查询本人作答、错题和掌握度；变化记录使用 `/mastery/{knowledgePointId}/changes` |
| `GET /recommendations` | 学生 | 查询本人推荐 |
| `POST /recommendations/{id}/accept`、`DELETE /recommendations/{id}` | 学生 | 幂等接受或忽略推荐 |
| `GET /photo-marking[/{id}]` | 学生 | 仅查询数据库已有记录，OCR 创建与识别暂未开放 |

服务间接口 `GET /internal/learning/practice-tasks/{taskId}` 仅供可信内网服务调用，不经过客户端直接访问。

## 数据库升级

不要对生产库自动执行 SQL。部署人员应先备份并在维护窗口按以下顺序操作：

1. 确认当前 `learning_db` 已包含原始 `schema.sql` 的 11 张业务表；先检查同一学生是否存在重复作业任务，若有重复，应人工核对并清理后再建立唯一索引。随后先部署教学服务中接收学习进度的内部接口。
2. 对已有数据库执行 `upgrade-online-closure.sql`。脚本在同一事务中先建立“学生 + 作业”唯一索引，再建立 `assignment_sync_event` 及其待投递、已投递索引；`ON_ERROR_STOP=1` 保证出错即停止。
3. 核对 `uk_practice_task_assignment_student`、`assignment_sync_event` 和两个事件索引均存在，再部署新版学习服务。
4. 观察学习服务调度日志和 `assignment_sync_event` 的 `PENDING` 数量；确认教学服务可用后再开放流量。

已有库执行：

```powershell
psql -v ON_ERROR_STOP=1 -d learning_db -f learning-service/src/main/resources/db/upgrade-online-closure.sql
```

全新空库只执行最新版 `schema.sql`，无需再执行升级脚本。升级脚本使用幂等 DDL，但执行前仍必须备份；本仓库不会自动连接或修改生产数据库。

## 验证

```powershell
.\mvnw.cmd -pl learning -am test
```
