package xyz.jiayuhan.userservice.model.request.account;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 学生本人可维护的学习上下文。 */
public record UpdateStudentProfileRequest(
        @NotNull(message = "年级不能为空")
        @Min(value = 1, message = "年级必须为 1-6")
        @Max(value = 6, message = "年级必须为 1-6")
        Integer gradeLevel,
        @NotNull(message = "学期不能为空")
        @Min(value = 1, message = "学期必须为 1 或 2")
        @Max(value = 2, message = "学期必须为 1 或 2")
        Integer semester
) {
}
