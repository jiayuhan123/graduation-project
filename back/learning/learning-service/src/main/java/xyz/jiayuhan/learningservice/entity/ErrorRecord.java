package xyz.jiayuhan.learningservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 一次错误诊断及其后续巩固状态。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("error_record")
public class ErrorRecord extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long answerRecordId;
    private Long studentId;
    private Long questionId;
    private Long knowledgePointId;
    private Long errorTypeId;
    private String errorPosition;
    private String diagnosis;
    private BigDecimal confidence;
    private String status;
    private Long resolvedByAnswerId;
    private OffsetDateTime resolvedAt;
}
