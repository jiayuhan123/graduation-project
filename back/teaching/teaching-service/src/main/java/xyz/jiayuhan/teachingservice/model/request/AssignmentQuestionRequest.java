package xyz.jiayuhan.teachingservice.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** 作业题目及其层级、顺序和分值。 */
public record AssignmentQuestionRequest(
        @NotNull(message = "题目编号不能为空") Long questionId,
        @NotBlank(message = "题目层级不能为空")
        @Pattern(regexp = "^[A-Z][A-Z0-9_-]{0,31}$", message = "题目层级格式不正确") String layerCode,
        @NotNull(message = "题目顺序不能为空") @Min(1) @Max(100) Integer sequenceNo,
        @NotNull(message = "题目分值不能为空")
        @DecimalMin(value = "0.01", message = "题目分值必须大于0")
        @DecimalMax(value = "999999.99", message = "题目分值过大") BigDecimal score
) {
}
