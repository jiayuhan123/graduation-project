package xyz.jiayuhan.contentservice.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 管理员处理题目反馈，可在解决反馈的同时下架题目。 */
public record HandleQuestionFeedbackRequest(
        @NotBlank(message = "处理动作不能为空")
        @Pattern(regexp = "PROCESS|RESOLVE|REJECT|RESOLVE_AND_DISABLE", message = "处理动作不正确")
        String action,
        @Size(max = 1000, message = "处理说明不能超过1000字")
        String comment,
        @NotNull(message = "版本号不能为空") @Min(value = 0, message = "版本号不能小于0")
        Integer version
) {
}
