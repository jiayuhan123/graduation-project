package xyz.jiayuhan.contentservice.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/** 修改题目请求；修改后题目自动回到草稿状态。 */
public record UpdateQuestionRequest(
        @NotBlank(message = "题目模式不能为空")
        @Pattern(regexp = "FIXED|TEMPLATE", message = "题目模式不正确") String questionMode,
        @NotBlank(message = "题型不能为空")
        @Pattern(regexp = "ARITHMETIC|FILL_BLANK|VERTICAL|CHOICE|JUDGEMENT", message = "题型不正确")
        String questionType,
        @NotBlank(message = "题干不能为空") String stem,
        String options,
        @NotBlank(message = "标准答案不能为空") String standardAnswer,
        String analysis,
        String generationRule,
        @NotNull(message = "难度不能为空") @Min(value = 1, message = "难度必须在1到5之间")
        @Max(value = 5, message = "难度必须在1到5之间") Integer difficulty,
        @Min(value = 1, message = "预计作答秒数必须大于0") Integer estimatedSeconds,
        @NotEmpty(message = "题目至少绑定一个知识点") List<@Valid QuestionKnowledgePointBindingRequest> knowledgePoints,
        @NotNull(message = "版本号不能为空") @Min(value = 0, message = "版本号不能小于0") Integer version
) {
}
