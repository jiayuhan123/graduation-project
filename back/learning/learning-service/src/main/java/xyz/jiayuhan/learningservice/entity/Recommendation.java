package xyz.jiayuhan.learningservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

import java.time.OffsetDateTime;

/** 可追踪来源和使用状态的个性化练习推荐。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recommendation")
public class Recommendation extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long studentId;
    private Long knowledgePointId;
    private String sourceType;
    private Long sourceId;
    private String reasonText;
    private Integer priority;
    private Integer recommendedQuestionCount;
    private String status;
    private Long generatedTaskId;
    private OffsetDateTime generatedAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime acceptedAt;
    private OffsetDateTime completedAt;
}
