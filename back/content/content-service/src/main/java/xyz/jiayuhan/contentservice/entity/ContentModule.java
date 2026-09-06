package xyz.jiayuhan.contentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.SoftDeleteVersionedEntity;

/** 年级、学期与学习主题组成的内容模块。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_module")
public class ContentModule extends SoftDeleteVersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
    private Integer gradeLevel;
    private Integer semester;
    private String learningTheme;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String publisher;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String editionDescription;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    private Integer sortOrder;
    private Integer status;
    private Long createdBy;
}
