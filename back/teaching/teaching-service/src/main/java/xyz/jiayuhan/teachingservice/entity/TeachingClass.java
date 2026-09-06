package xyz.jiayuhan.teachingservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;

/** 教师组织作业和班级学情的教学班。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teaching_class")
public class TeachingClass extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String classCode;
    private String name;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String schoolName;
    private String academicYear;
    private Integer gradeLevel;
    private Integer semester;
    private String status;
    private Long createdBy;
}
