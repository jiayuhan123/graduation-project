package xyz.jiayuhan.gameservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 一个学生在一个关卡上的解锁与最佳成绩。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_level_progress")
public class GameLevelProgress extends AuditableEntity {

    private Long studentId;
    private Long levelId;
    private String status;
    private Integer attemptCount;
    private BigDecimal bestScore;
    private Integer bestCorrectCount;
    private Integer bestStarCount;
    private Integer bestDurationSeconds;
    private Long lastSessionId;
    private OffsetDateTime firstUnlockedAt;
    private OffsetDateTime firstPassedAt;
    private OffsetDateTime lastPlayedAt;
}
