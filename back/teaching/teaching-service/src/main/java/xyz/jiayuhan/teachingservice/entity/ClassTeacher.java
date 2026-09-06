package xyz.jiayuhan.teachingservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

import java.time.OffsetDateTime;

/** 教学班与教师关联。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("class_teacher")
public class ClassTeacher extends AuditableEntity {

    private Long classId;
    private Long teacherId;
    private String teacherRole;
    private OffsetDateTime joinedAt;
}
