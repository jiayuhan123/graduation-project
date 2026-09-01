package xyz.jiayuhan.userservice.model.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员修改用户资料请求，不允许通过该接口修改密码、状态和角色。
 *
 * @param nickname 昵称；空白值用于清空字段
 * @param realName 真实姓名；空白值用于清空字段
 * @param email 邮箱；空白值用于清空字段，非空时全局唯一
 * @param phone 手机号；空白值用于清空字段，非空时全局唯一
 * @param avatarUrl 头像地址；空白值用于清空字段
 */
public record AdminUpdateUserRequest(
        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname,
        @Size(max = 50, message = "真实姓名不能超过 50 个字符")
        String realName,
        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱不能超过 254 个字符")
        String email,
        @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
        String phone,
        @Size(max = 500, message = "头像地址不能超过 500 个字符")
        String avatarUrl
) {
}
