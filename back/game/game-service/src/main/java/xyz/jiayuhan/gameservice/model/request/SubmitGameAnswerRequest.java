package xyz.jiayuhan.gameservice.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 游戏逐题作答请求；requestId 用于安全重试。 */
public record SubmitGameAnswerRequest(
        @NotBlank(message = "请求幂等键不能为空")
        @Size(min = 8, max = 64, message = "请求幂等键长度必须在8到64之间")
        String requestId,

        @NotBlank(message = "答案不能为空")
        @Size(max = 2000, message = "答案内容过长")
        String answer,

        @NotNull(message = "答题耗时不能为空")
        @Min(value = 0, message = "答题耗时不能为负数")
        @Max(value = 7_200_000, message = "答题耗时不能超过两小时")
        Integer responseMs
) {
}
