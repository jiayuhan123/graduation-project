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

/** 一次单人或团队闯关对局。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "game_session", autoResultMap = true)
public class GameSession extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long levelId;
    private String mode;
    private Long creatorStudentId;
    private String status;
    private Integer memberCount;
    private Integer questionCountSnapshot;
    private Integer timeLimitSecondsSnapshot;
    private BigDecimal passRateSnapshot;
    private BigDecimal teamProgress;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String resultDetail;
}
