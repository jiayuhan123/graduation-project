package xyz.jiayuhan.learningservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

/** 练习任务与目标知识点关联。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_task_knowledge_point")
public class PracticeTaskKnowledgePoint extends AuditableEntity {

    private Long taskId;
    private Long knowledgePointId;
    private Integer targetQuestionCount;
}
