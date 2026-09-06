package xyz.jiayuhan.contentservice.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 题目绑定知识点请求。 */
public record QuestionKnowledgePointBindingRequest(
        @NotNull(message = "知识点不能为空") Long knowledgePointId,
        @NotNull(message = "是否主知识点不能为空") Boolean primary,
        @NotNull(message = "知识点权重不能为空")
        @DecimalMin(value = "0.0001", message = "知识点权重必须大于0")
        @DecimalMax(value = "1.0000", message = "知识点权重不能超过1") BigDecimal weight
) {
}
