package xyz.jiayuhan.userservice.model.request.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 当前用户修改密码请求。
 *
 * @param oldPassword 用于身份复核的当前明文密码
 * @param newPassword 新明文密码，不允许与当前密码相同
 */
public record ChangePasswordRequest(
        @NotBlank(message = "旧密码不能为空")
        String oldPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 72, message = "新密码长度为 6-72 个字符")
        String newPassword
) {
}
