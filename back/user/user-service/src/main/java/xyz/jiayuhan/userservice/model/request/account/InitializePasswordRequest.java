package xyz.jiayuhan.userservice.model.request.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 微信账号首次设置密码请求。 */
public record InitializePasswordRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 72, message = "密码长度为 6-72 个字符")
        String newPassword
) {
}
