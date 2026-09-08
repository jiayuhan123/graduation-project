package xyz.jiayuhan.gameservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;

import java.time.OffsetDateTime;

/** 玩家跨关卡累计的积分、经验与统计。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_player_progress")
public class GamePlayerProgress extends VersionedEntity {

    @TableId(type = IdType.INPUT)
    private Long studentId;
    private Long totalPoints;
    private Long experience;
    private Integer playerLevel;
    private Integer totalSessions;
    private Integer successSessions;
    private Integer levelPassedCount;
    private Integer pkSessions;
    private Integer pkWins;
    private Integer pkLosses;
    private Integer pkDraws;
    private Integer pkRating;
    private Integer currentWinStreak;
    private Integer bestWinStreak;
    private Integer bestCombo;
    private OffsetDateTime lastPlayedAt;
}
