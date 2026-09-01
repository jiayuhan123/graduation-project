package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.OffsetDateTime;

/** 学生角色的专有学习上下文；班级关系属于 teaching-service。 */
@Data
@TableName("sys_student_profile")
public class StudentProfile {

    @TableId(type = IdType.INPUT)
    private Long userId;

    private String studentNo;

    private Integer gradeLevel;

    private Integer semester;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @Version
    private Integer version;
}
