package xyz.jiayuhan.userservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求。
 *
 * @param username 登录账号，可填写用户名、手机号或邮箱；字段名为兼容既有客户端保留
 * @param password 明文密码，仅用于本次凭证校验
 */
public record LoginDTO(
        @NotBlank(message = "账号不能为空")
        @Size(max = 254, message = "账号长度不能超过 254 个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 72, message = "密码长度不能超过 72 个字符")
        String password
) {
}
