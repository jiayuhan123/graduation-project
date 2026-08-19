package xyz.jiayuhan.userservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建自定义角色请求。
 *
 * @param code 稳定的角色编码，用于程序鉴权，创建后不可修改
 * @param name 面向管理端展示的角色名称
 * @param description 角色用途说明，可为空
 */
public record CreateRoleDTO(
        @NotBlank(message = "角色编码不能为空")
        @Size(min = 2, max = 64, message = "角色编码长度为 2-64 个字符")
        @Pattern(regexp = "^[a-z][a-z0-9_-]*$", message = "角色编码只能包含小写字母、数字、下划线和横线")
        String code,
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64, message = "角色名称不能超过 64 个字符")
        String name,
        @Size(max = 255, message = "角色描述不能超过 255 个字符")
        String description
) {
}
