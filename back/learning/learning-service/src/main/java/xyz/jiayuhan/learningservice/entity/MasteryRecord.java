package xyz.jiayuhan.learningservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 学生在一个知识点上的当前掌握状态。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mastery_record")
public class MasteryRecord extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long studentId;
    private Long knowledgePointId;
    private BigDecimal masteryScore;
    private BigDecimal accuracyRate;
    private Integer avgResponseMs;
    private Integer totalAnswerCount;
    private Integer correctAnswerCount;
    private Integer consecutiveCorrectCount;
    private OffsetDateTime lastPracticedAt;
    private OffsetDateTime nextReviewAt;
}
