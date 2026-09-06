package xyz.jiayuhan.teachingservice.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 修改教学班请求，班级码不可修改。 */
public record UpdateTeachingClassRequest(
        @NotBlank(message = "班级名称不能为空")
        @Size(max = 100, message = "班级名称不能超过100个字符") String name,
        @Size(max = 150, message = "学校名称不能超过150个字符") String schoolName,
        @NotBlank(message = "学年不能为空")
        @Pattern(regexp = "^[0-9]{4}-[0-9]{4}$", message = "学年格式应为2026-2027") String academicYear,
        @NotNull(message = "年级不能为空") @Min(1) @Max(6) Integer gradeLevel,
        @NotNull(message = "学期不能为空") @Min(1) @Max(2) Integer semester,
        @NotNull(message = "版本号不能为空") @Min(0) Integer version
) {
}
