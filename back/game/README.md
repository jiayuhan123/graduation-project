# Game 游戏闯关领域

`game` 是游戏闯关领域的 Maven 聚合模块，本身不运行，也不注册到 Nacos。

```text
game
├─ game-api      跨服务 HTTP 契约、DTO 与稳定路径常量
└─ game-service  游戏闯关服务，默认端口 8008，使用 game_db
```

当前数据库基线同时面向单人闯关和双人 PK：

- `game_level` 与 `game_level_question`：配置关卡规则，并支持按知识点随机选题或策划固定题池；
- `game_session`：统一保存闯关/PK 的年级、知识点、难度、题量和时限快照；闯关必须关联关卡，PK 不依赖关卡；
- `game_match_ticket`：保存双人 PK 随机匹配条件，房间模式直接使用对局邀请码；
- `game_session_member` 与 `game_session_question`：保存双方状态、固定题目快照、幂等作答、连击、耗时和结算；
- `game_player_progress`、`game_level_progress` 与 `game_reward_record`：分别保存综合成长/PK 评分、关卡进度和幂等奖励流水；
- `game_event_outbox`：为后续向学习服务同步错题、掌握度等结果提供可靠事件出口；
- 10 张游戏库表对应的 Entity、Mapper，并保留已启用关卡、本人对局、本人题目过程、玩家/关卡进度与奖励流水查询；
- 游戏对局内部查询契约与 `GameFeignClient`；
- 对局成员归属校验、题目快照隔离和数据库映射覆盖测试。

当前已经完成单人闯关和双人 PK 的后端命令闭环。PK 固定为两人；闯关固定为单人。两种模式都只使用内容服务中已经启用的固定题目，并通过事务发件箱可靠发布逐题结果和对局结算事件。

核心能力：

- 创建、开始、取消和限时完成单人闯关；校验前置关卡，通过后解锁后续关卡；
- 创建邀请码房间、加入房间、按相同年级/知识点/难度/题量/时限随机匹配；
- PK 双方使用完全相同的固定题目快照，逐题立即判题；结算依次比较得分、正确数、作答数和总耗时，避免超时未作答者因耗时为零获胜；
- 作答请求幂等、对局行锁、随机匹配 `SKIP LOCKED`、玩家成长乐观锁和奖励唯一键；
- 闯关星级/积分/经验、PK 战绩/Elo 评分/连胜、超时自动结算；
- 答案和对局完成事件与业务结算同事务入箱，并在 RabbitMQ 失败时退避重试。

学生命令接口：

- `POST /api/games/levels/{levelId}/sessions`
- `POST /api/games/pk/rooms`
- `POST /api/games/pk/rooms/{inviteCode}/join`
- `POST /api/games/pk/matches`
- `DELETE /api/games/pk/matches/{ticketId}`
- `POST /api/games/sessions/{sessionId}/start`
- `POST /api/games/sessions/{sessionId}/questions/{questionId}/answers`
- `POST /api/games/sessions/{sessionId}/finish`
- `DELETE /api/games/sessions/{sessionId}`

PK 未传 `questionCount` 时默认 1 题、未传 `timeLimitSeconds` 时默认 180 秒。当前固定题库按计算知识点的题量较少；显式增加题量时，如果指定知识点的可用固定题不足，接口会直接返回题池不足，不会生成或重复使用题目。

## 数据库脚本

- 全新空库执行 `game-service/src/main/resources/db/schema.sql`。
- `game-service/src/main/resources/db/rebuild-pk-and-level.sql` 会删除游戏领域旧表，只允许在已经确认所有游戏表均为空的开发或测试库执行，严禁用于需要保留数据的生产库。
- 内容服务一至六年级固定题库脚本执行完成后，可执行 `game-service/src/main/resources/db/seed-pep-levels.sql`，初始化每个年级一个 5 题体验关。脚本可重复执行。

```powershell
psql -v ON_ERROR_STOP=1 --single-transaction -d game_db -f game-service/src/main/resources/db/schema.sql
```

空开发库重建：

```powershell
psql -v ON_ERROR_STOP=1 -d game_db -f game-service/src/main/resources/db/rebuild-pk-and-level.sql
```

初始化体验关：

```powershell
psql -v ON_ERROR_STOP=1 -d game_db -f game-service/src/main/resources/db/seed-pep-levels.sql
```

```powershell
.\mvnw.cmd -pl game -am test
```
