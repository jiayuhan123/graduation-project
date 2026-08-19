package xyz.jiayuhan.userservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员重置其他用户密码请求。
 *
 * @param newPassword 新明文密码，重置成功后目标账号的已有会话全部失效
 */
public record ResetPasswordDTO(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 72, message = "新密码长度为 6-72 个字符")
        String newPassword
) {
}
