package xyz.jiayuhan.contentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.SoftDeleteVersionedEntity;

/** 可按父子关系组织的知识点。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_point")
public class KnowledgePoint extends SoftDeleteVersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long moduleId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long parentId;
    private String code;
    private String name;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    private Integer difficultyLevel;
    private Integer sortOrder;
    private Integer status;
    private Long createdBy;
}
