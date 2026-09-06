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

/** 练习生成时固化的题目快照，避免题库后续变化影响历史记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "practice_task_question", autoResultMap = true)
public class PracticeTaskQuestion extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long taskId;
    private Long questionId;
    private Integer sequenceNo;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String questionSnapshot;
    private BigDecimal maxScore;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String sourceDetail;
}
