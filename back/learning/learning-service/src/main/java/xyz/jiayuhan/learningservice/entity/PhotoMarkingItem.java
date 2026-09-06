package xyz.jiayuhan.learningservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;
import xyz.jiayuhan.common.data.typehandler.JsonbTypeHandler;

import java.math.BigDecimal;
import java.util.UUID;

/** 拍照批改中切分出的单题识别结果。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "photo_marking_item", autoResultMap = true)
public class PhotoMarkingItem extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long markingTaskId;
    private Integer itemNo;
    private UUID cropFileId;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String boundingBox;
    private String recognizedStem;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String recognizedAnswer;
    private Long matchedQuestionId;
    private BigDecimal recognitionConfidence;
    private Long taskQuestionId;
    private Long answerRecordId;
    private String status;
}
