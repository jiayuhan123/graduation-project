package xyz.jiayuhan.userservice.model.request.account;

import jakarta.validation.constraints.Size;

/** 教师本人可维护的公开职业资料。 */
public record UpdateTeacherProfileRequest(
        @Size(max = 50, message = "职称不能超过 50 个字符")
        String title,
        @Size(max = 500, message = "简介不能超过 500 个字符")
        String introduction
) {
}
