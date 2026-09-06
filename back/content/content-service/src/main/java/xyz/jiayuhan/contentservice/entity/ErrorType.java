package xyz.jiayuhan.contentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.SoftDeleteVersionedEntity;

/** 诊断流程使用的标准错因字典。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("error_type")
public class ErrorType extends SoftDeleteVersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
    private String category;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String applicableQuestionType;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String explanationTemplate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remediationHint;
    private Integer status;
}
