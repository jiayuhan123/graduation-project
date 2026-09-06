package xyz.jiayuhan.contentservice.model.response;

import java.math.BigDecimal;

/** 管理端题目知识点绑定。 */
public record QuestionKnowledgePointBindingResponse(
        Long knowledgePointId,
        String knowledgePointCode,
        String knowledgePointName,
        Boolean primary,
        BigDecimal weight
) {
}
