package xyz.jiayuhan.contentservice.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 创建错因类型请求。 */
public record CreateErrorTypeRequest(
        @NotBlank(message = "错因编码不能为空")
        @Size(max = 64, message = "错因编码不能超过64个字符")
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9._-]*$", message = "错因编码格式不正确") String code,
        @NotBlank(message = "错因名称不能为空")
        @Size(max = 100, message = "错因名称不能超过100个字符") String name,
        @NotBlank(message = "错因分类不能为空")
        @Pattern(regexp = "CALCULATION|PLACE_VALUE|CARRY|BORROW|FORMAT|CONCEPT|OTHER", message = "错因分类不正确")
        String category,
        @Pattern(regexp = "ARITHMETIC|FILL_BLANK|VERTICAL|CHOICE|JUDGEMENT", message = "适用题型不正确")
        String applicableQuestionType,
        @Size(max = 500, message = "错因说明不能超过500个字符") String description,
        @Size(max = 1000, message = "解释模板不能超过1000个字符") String explanationTemplate,
        @Size(max = 1000, message = "补救提示不能超过1000个字符") String remediationHint,
        @NotNull(message = "状态不能为空") @Min(value = 0, message = "状态只能为0或1")
        @Max(value = 1, message = "状态只能为0或1") Integer status
) {
}
