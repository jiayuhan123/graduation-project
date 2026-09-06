package xyz.jiayuhan.contentservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

import java.math.BigDecimal;

/** 题目与知识点关联；两个 ID 共同组成数据库主键。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_knowledge_point")
public class QuestionKnowledgePoint extends AuditableEntity {

    private Long questionId;
    private Long knowledgePointId;
    @TableField("is_primary")
    private Boolean primary;
    private BigDecimal weight;
}
