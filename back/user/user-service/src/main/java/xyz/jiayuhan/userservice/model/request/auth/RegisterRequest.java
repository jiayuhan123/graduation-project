package xyz.jiayuhan.userservice.model.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 通用账号注册请求；手机号和邮箱均为可选字段，可按后续毕设业务继续收紧规则。
 *
 * @param username 用户名，保存前统一转换为小写
 * @param password 明文密码，服务层只保存 BCrypt 哈希
 * @param nickname 昵称，可为空
 * @param realName 真实姓名，可为空
 * @param email 邮箱，可为空；非空时全局唯一且按小写保存
 * @param phone 手机号，可为空；非空时全局唯一
 * @param gradeLevel 学生当前年级，1-6
 * @param semester 当前学期，1-2
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度为 3-32 个字符")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "用户名须以字母开头且只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 72, message = "密码长度为 6-72 个字符")
        String password,

        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname,

        @Size(max = 50, message = "真实姓名不能超过 50 个字符")
        String realName,

        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱不能超过 254 个字符")
        String email,

        @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
        String phone,

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
