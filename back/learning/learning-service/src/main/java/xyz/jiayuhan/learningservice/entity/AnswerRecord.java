package xyz.jiayuhan.learningservice.entity;

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

/** 在线、OCR 与游戏入口共用的统一答题记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "answer_record", autoResultMap = true)
public class AnswerRecord extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long taskQuestionId;
    private Long studentId;
    private Integer attemptNo;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String studentAnswer;
    private String gradingStatus;
    @TableField("is_correct")
    private Boolean correct;
    private BigDecimal score;
    private Integer responseMs;
    private String answerSource;
    private BigDecimal recognitionConfidence;
    private OffsetDateTime submittedAt;
    private OffsetDateTime gradedAt;
}
