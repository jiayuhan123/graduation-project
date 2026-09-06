package xyz.jiayuhan.teachingservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 一个学生对一份作业的完成与评分摘要。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assignment_submission")
public class AssignmentSubmission extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String layerCode;
    private Long practiceTaskId;
    private String status;
    private BigDecimal score;
    private Integer correctCount;
    private Integer totalCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private OffsetDateTime gradedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String teacherComment;
}
