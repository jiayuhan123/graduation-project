package xyz.jiayuhan.userservice.model.request.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 管理员维护学生资料请求。 */
public record AdminStudentProfileRequest(
        @Size(max = 32, message = "学号不能超过 32 个字符")
        String studentNo,
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
