package xyz.jiayuhan.contentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.SoftDeleteVersionedEntity;
import xyz.jiayuhan.common.data.typehandler.JsonbTypeHandler;

import java.time.OffsetDateTime;

/** 固定题目或参数化生成模板。JSONB 字段使用 JSON 文本承载。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "question", autoResultMap = true)
public class Question extends SoftDeleteVersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String questionMode;
    private String questionType;
    private String stem;
    @TableField(typeHandler = JsonbTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private String options;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String standardAnswer;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String analysis;
    @TableField(typeHandler = JsonbTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private String generationRule;
    private Integer difficulty;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer estimatedSeconds;
    private String status;
    private Long createdBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long reviewedBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime reviewedAt;
}
