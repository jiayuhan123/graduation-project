package xyz.jiayuhan.gameservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;
import xyz.jiayuhan.common.data.typehandler.JsonbTypeHandler;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 一个成员在一次对局中的题目和推进结果。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "game_session_question", autoResultMap = true)
public class GameSessionQuestion extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long sessionId;
    private Long studentId;
    private Integer sequenceNo;
    private Long questionId;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String questionSnapshot;
    private Long answerRecordId;
    @TableField("is_correct")
    private Boolean correct;
    private BigDecimal progressDelta;
    private Integer energyDelta;
    private OffsetDateTime answeredAt;
}
