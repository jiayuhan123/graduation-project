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
    private String status;
    private Long practiceTaskId;
    private Integer answeredCount;
    private Integer correctCount;
    private BigDecimal score;
    private BigDecimal contributionValue;
    private Integer maxCombo;
    private OffsetDateTime joinedAt;
    private OffsetDateTime finishedAt;
}
