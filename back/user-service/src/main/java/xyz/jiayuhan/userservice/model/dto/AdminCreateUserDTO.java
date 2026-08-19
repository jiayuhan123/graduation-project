package xyz.jiayuhan.userservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 管理员创建用户请求。
 *
 * @param username 用户名，保存前统一转换为小写
 * @param password 初始明文密码，服务层只保存 BCrypt 哈希
 * @param nickname 昵称，可为空
 * @param realName 真实姓名，可为空
 * @param email 邮箱，可为空；非空时全局唯一且按小写保存
 * @param phone 手机号，可为空；非空时全局唯一
 * @param status 初始状态：1 正常、0 禁用；为空时默认正常
 * @param roleIds 初始角色 ID；为空或空集合时保留注册流程分配的默认角色
 */
public record AdminCreateUserDTO(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度为 3-32 个字符")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "用户名格式不正确")
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
        @Min(value = 0, message = "状态只能为 0 或 1")
        @Max(value = 1, message = "状态只能为 0 或 1")
        Integer status,
        List<Long> roleIds
) {
}
