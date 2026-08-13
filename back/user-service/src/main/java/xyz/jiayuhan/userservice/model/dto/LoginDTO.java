package xyz.jiayuhan.userservice.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求参数
 *
 * @param username 用户名或手机号
 * @param password 登录密码（明文传输，服务端 BCrypt 比对）
 */
public record LoginDTO(

        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
