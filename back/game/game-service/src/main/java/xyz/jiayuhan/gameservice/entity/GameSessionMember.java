package xyz.jiayuhan.gameservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 对局成员的个人状态和贡献。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_session_member")
public class GameSessionMember extends AuditableEntity {

    private Long sessionId;
    private Long studentId;
    private Integer seatNo;
    private String memberRole;
    private String status;
    private String result;
    private Long practiceTaskId;
    private Integer answeredCount;
    private Integer correctCount;
    private BigDecimal score;
    private BigDecimal contributionValue;
    private Integer currentCombo;
    private Integer maxCombo;
    private Long totalResponseMs;
    private Integer ratingBefore;
    private Integer ratingAfter;
    private Integer pointsReward;
    private Integer experienceReward;
    private OffsetDateTime joinedAt;
    private OffsetDateTime lastAnsweredAt;
    private OffsetDateTime finishedAt;
}
