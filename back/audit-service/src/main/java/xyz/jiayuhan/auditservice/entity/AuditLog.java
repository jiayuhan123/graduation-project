package xyz.jiayuhan.auditservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 由跨服务领域事件生成的不可变操作审计记录。 */
@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.INPUT)
    private UUID id;

    /** 来源事件 ID，用于消费幂等和端到端排障。 */
    private UUID eventId;

    private Long operatorId;
    private String source;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private OffsetDateTime occurredAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
