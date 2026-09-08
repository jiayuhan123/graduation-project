package xyz.jiayuhan.gameservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;
import xyz.jiayuhan.common.data.typehandler.JsonbTypeHandler;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 一次单人闯关或双人 PK 对局。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "game_session", autoResultMap = true)
public class GameSession extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long levelId;
    private String mode;
    private Long creatorStudentId;
    private String matchMode;
    private String inviteCode;
    private String status;
    private Integer memberCount;
    private Integer gradeLevelSnapshot;
    private Long knowledgePointIdSnapshot;
    private Integer difficultySnapshot;
    private Integer questionCountSnapshot;
    private Integer timeLimitSecondsSnapshot;
    private BigDecimal passRateSnapshot;
    @TableField("progress_value")
    private BigDecimal teamProgress;
    private Long winnerStudentId;
    private String resultType;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime settledAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String resultDetail;
}
