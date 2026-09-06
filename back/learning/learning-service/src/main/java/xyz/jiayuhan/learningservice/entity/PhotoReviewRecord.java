package xyz.jiayuhan.learningservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;
import xyz.jiayuhan.common.data.typehandler.JsonbTypeHandler;

import java.time.OffsetDateTime;

/** 教师对低可信拍照识别结果的复核历史。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "photo_review_record", autoResultMap = true)
public class PhotoReviewRecord extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long markingTaskId;
    private Long markingItemId;
    private Long reviewerId;
    private String decision;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String beforeData;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String afterData;
    private String comment;
    private OffsetDateTime reviewedAt;
}
