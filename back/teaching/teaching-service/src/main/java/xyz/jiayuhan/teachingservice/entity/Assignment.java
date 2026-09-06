package xyz.jiayuhan.teachingservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;
import xyz.jiayuhan.common.data.typehandler.JsonbTypeHandler;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 教师布置的统一或分层作业。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "assignment", autoResultMap = true)
public class Assignment extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long classId;
    private Long teacherId;
    private String title;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    private String assignmentType;
    private String status;
    private OffsetDateTime startAt;
    private OffsetDateTime dueAt;
    private BigDecimal totalScore;
    @TableField(typeHandler = JsonbTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private String settings;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private OffsetDateTime publishedAt;
}
