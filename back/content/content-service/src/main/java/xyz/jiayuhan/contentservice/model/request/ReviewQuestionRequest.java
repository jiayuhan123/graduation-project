package xyz.jiayuhan.contentservice.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 审核、退回或下架题目请求。 */
public record ReviewQuestionRequest(
        @NotBlank(message = "审核决定不能为空")
        @Pattern(regexp = "APPROVE|RETURN|DISABLE", message = "审核决定不正确") String decision,
        @Size(max = 500, message = "审核说明不能超过500个字符") String remark,
        @NotNull(message = "版本号不能为空") @Min(value = 0, message = "版本号不能小于0") Integer version
) {
}
