package xyz.jiayuhan.contentservice.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 学生或教师提交题目质量问题。 */
public record CreateQuestionFeedbackRequest(
        @NotBlank(message = "反馈类型不能为空")
        @Pattern(regexp = "STEM_ERROR|ANSWER_ERROR|AMBIGUOUS|FORMAT_ERROR|OTHER", message = "反馈类型不正确")
        String category,
        @NotBlank(message = "反馈说明不能为空")
        @Size(max = 1000, message = "反馈说明不能超过1000字")
        String description
) {
}
