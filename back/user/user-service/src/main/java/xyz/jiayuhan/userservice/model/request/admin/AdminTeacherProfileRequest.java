package xyz.jiayuhan.userservice.model.request.admin;

import jakarta.validation.constraints.Size;

/** 管理员维护教师资料请求。 */
public record AdminTeacherProfileRequest(
        @Size(max = 32, message = "教师工号不能超过 32 个字符")
        String teacherNo,
        @Size(max = 50, message = "职称不能超过 50 个字符")
        String title,
        @Size(max = 500, message = "简介不能超过 500 个字符")
        String introduction
) {
}
