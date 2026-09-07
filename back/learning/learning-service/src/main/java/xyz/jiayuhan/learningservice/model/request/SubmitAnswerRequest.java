package xyz.jiayuhan.learningservice.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 保存一次手工作答。 */
public record SubmitAnswerRequest(
        @NotBlank(message = "答案不能为空")
        @Size(max = 200, message = "答案不能超过200个字符") String answer,
        @Min(value = 0, message = "作答耗时不能小于0")
        @Max(value = 7200000, message = "单题作答耗时不能超过120分钟") Integer responseMs
) {
}
