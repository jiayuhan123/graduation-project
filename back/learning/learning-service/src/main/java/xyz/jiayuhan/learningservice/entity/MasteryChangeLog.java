package xyz.jiayuhan.learningservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 掌握度变化的不可变解释记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mastery_change_log")
public class MasteryChangeLog extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long masteryId;
    private Long answerRecordId;
    private BigDecimal beforeScore;
    private BigDecimal afterScore;
    private BigDecimal deltaScore;
    private String reason;
    private OffsetDateTime occurredAt;
}
