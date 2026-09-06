package xyz.jiayuhan.learningservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 学生一次练习流程的聚合根。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_task")
public class PracticeTask extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long studentId;
    private String sourceType;
    private String sourceId;
    private String title;
    private String mode;
    private String status;
    private Integer questionCount;
    private Integer timeLimitSeconds;
    private BigDecimal targetAccuracy;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime expiresAt;
}
