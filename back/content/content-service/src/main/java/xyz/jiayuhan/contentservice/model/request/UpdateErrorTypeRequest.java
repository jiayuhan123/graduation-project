package xyz.jiayuhan.contentservice.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 修改错因类型请求；稳定编码不可修改。 */
public record UpdateErrorTypeRequest(
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
        @NotNull(message = "版本号不能为空") @Min(value = 0, message = "版本号不能小于0") Integer version
) {
}
