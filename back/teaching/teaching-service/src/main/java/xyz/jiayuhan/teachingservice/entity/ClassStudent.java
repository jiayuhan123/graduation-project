package xyz.jiayuhan.teachingservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

import java.time.OffsetDateTime;

/** 教学班与学生关联及在班状态。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("class_student")
public class ClassStudent extends AuditableEntity {

    private Long classId;
    private Long studentId;
    private String studentNoInClass;
    private String status;
    private OffsetDateTime joinedAt;
    private OffsetDateTime leftAt;
}
