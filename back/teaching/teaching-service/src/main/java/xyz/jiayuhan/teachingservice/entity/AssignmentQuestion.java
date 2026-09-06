package xyz.jiayuhan.teachingservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;
import xyz.jiayuhan.common.data.typehandler.JsonbTypeHandler;

import java.math.BigDecimal;

/** 作业分层中的题目快照。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "assignment_question", autoResultMap = true)
public class AssignmentQuestion extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long assignmentId;
    private String layerCode;
    private Long questionId;
    private Integer sequenceNo;
    private BigDecimal score;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String questionSnapshot;
}
