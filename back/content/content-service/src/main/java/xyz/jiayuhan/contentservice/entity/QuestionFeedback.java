package xyz.jiayuhan.contentservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;

import java.time.OffsetDateTime;

/** 用户提交的题目质量反馈及管理员处理状态。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_feedback")
public class QuestionFeedback extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long questionId;
    private Long reporterId;
    private String category;
    private String description;
    private String status;
    private Long handlerId;
    private String handleComment;
    private OffsetDateTime handledAt;
}
