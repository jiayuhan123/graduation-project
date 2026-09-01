package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.OffsetDateTime;

/** 教师角色的专有资料；班级、作业等领域数据不进入用户服务。 */
@Data
@TableName("sys_teacher_profile")
public class TeacherProfile {

    @TableId(type = IdType.INPUT)
    private Long userId;

    private String teacherNo;

    private String title;

    private String introduction;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @Version
    private Integer version;
}
