package xyz.jiayuhan.learningservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 一次图片上传、识别、批改与复核流程。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("photo_marking_task")
public class PhotoMarkingTask extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long studentId;
    private UUID originalFileId;
    private Long practiceTaskId;
    private String status;
    private Integer pageCount;
    private BigDecimal overallConfidence;
    private Integer recognizedItemCount;
    private Long reviewerId;
    private String reviewStatus;
    private OffsetDateTime submittedAt;
    private OffsetDateTime processedAt;
    private OffsetDateTime reviewedAt;
    private String failureReason;
}
