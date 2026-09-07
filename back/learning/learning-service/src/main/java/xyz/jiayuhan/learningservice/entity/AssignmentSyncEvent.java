package xyz.jiayuhan.learningservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 学习库内可靠回写教学服务作业进度的事务发件箱。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assignment_sync_event")
public class AssignmentSyncEvent extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private Long practiceTaskId;
    private String progressStatus;
    private BigDecimal score;
    private Integer correctCount;
    private Integer totalCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private OffsetDateTime gradedAt;
    private String deliveryStatus;
    private Integer retryCount;
    private OffsetDateTime nextRetryAt;
    private OffsetDateTime syncedAt;
    private String lastError;
}
