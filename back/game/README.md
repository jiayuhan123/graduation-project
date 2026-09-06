# Game 游戏闯关领域

`game` 是游戏闯关领域的 Maven 聚合模块，本身不运行，也不注册到 Nacos。

```text
game
├─ game-api      跨服务 HTTP 契约、DTO 与稳定路径常量
└─ game-service  游戏闯关服务，默认端口 8008，使用 game_db
```

当前已提供：

- 7 张游戏库表对应的 Entity、Mapper 和两个复合键模型；
- 已启用关卡、本人对局、本人题目过程、玩家/关卡进度与奖励流水查询；
- 游戏对局内部查询契约与 `GameFeignClient`；
- 对局成员归属校验、题目快照隔离和数据库映射覆盖测试。

组队、题目生成、实时推进、结算、解锁及奖励发放属于后续命令用例，必须结合学习服务结果和幂等键实现。

```powershell
.\mvnw.cmd -pl game -am test
```
