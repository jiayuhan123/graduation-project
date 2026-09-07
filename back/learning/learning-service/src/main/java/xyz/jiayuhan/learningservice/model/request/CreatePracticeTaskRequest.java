package xyz.jiayuhan.learningservice.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** 学生创建自主练习时提交的组题条件。 */
public record CreatePracticeTaskRequest(
        @NotNull(message = "请选择知识点") Long knowledgePointId,
        @Min(value = 1, message = "难度最低为1")
        @Max(value = 5, message = "难度最高为5") Integer difficulty,
        @Min(value = 1, message = "至少选择1道题")
        @Max(value = 20, message = "一次最多选择20道题") Integer questionCount,
        @Pattern(regexp = "NORMAL|TIMED|SPECIAL|REVIEW", message = "练习模式不正确") String mode,
        @Min(value = 30, message = "限时练习至少30秒")
        @Max(value = 7200, message = "限时练习最多120分钟") Integer timeLimitSeconds,
        @DecimalMin(value = "0.01", message = "目标正确率必须大于0")
        @DecimalMax(value = "1.00", message = "目标正确率不能超过1") BigDecimal targetAccuracy
) {
}
