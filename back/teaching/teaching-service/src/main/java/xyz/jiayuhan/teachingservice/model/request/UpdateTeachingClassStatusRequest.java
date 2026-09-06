package xyz.jiayuhan.teachingservice.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 修改教学班状态请求。 */
public record UpdateTeachingClassStatusRequest(
        @NotBlank(message = "班级状态不能为空")
        @Pattern(regexp = "ACTIVE|ARCHIVED", message = "班级状态不正确") String status,
        @NotNull(message = "版本号不能为空") @Min(0) Integer version
) {
}
