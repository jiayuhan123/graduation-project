package xyz.jiayuhan.userservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求参数
 *
 * @param username 用户名（3-20 位，仅字母/数字/下划线）
 * @param password 登录密码（6-20 位，服务端 BCrypt 加密）
 * @param realName 真实姓名（选填）
 * @param phone    手机号（1 开头 11 位数字）
 */
public record RegisterDTO(

        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度为 3-20 个字符")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度为 6-20 个字符")
        String password,

        @Size(max = 50, message = "真实姓名不能超过 50 个字符")
        String realName,

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone
) {
}
