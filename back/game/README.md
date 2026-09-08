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

当前只完成数据库和查询基线，创建对局、PK 匹配、固定题库组题、实时推进、结算、解锁及奖励发放仍属于后续命令用例。PK 固定为两人；闯关固定为单人。两种模式都必须只使用内容服务中已经启用的固定题目，并通过事务发件箱可靠同步学习结果。

## 数据库脚本

- 全新空库执行 `game-service/src/main/resources/db/schema.sql`。
- `game-service/src/main/resources/db/rebuild-pk-and-level.sql` 会删除游戏领域旧表，只允许在已经确认所有游戏表均为空的开发或测试库执行，严禁用于需要保留数据的生产库。

```powershell
psql -v ON_ERROR_STOP=1 --single-transaction -d game_db -f game-service/src/main/resources/db/schema.sql
```

空开发库重建：

```powershell
psql -v ON_ERROR_STOP=1 -d game_db -f game-service/src/main/resources/db/rebuild-pk-and-level.sql
```

```powershell
.\mvnw.cmd -pl game -am test
```
