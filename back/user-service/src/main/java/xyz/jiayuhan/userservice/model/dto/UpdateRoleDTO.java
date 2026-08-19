package xyz.jiayuhan.userservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改角色展示信息请求；角色编码是鉴权标识，不通过该接口修改。
 *
 * @param name 角色展示名称
 * @param description 角色用途说明；空白值用于清空字段
 */
public record UpdateRoleDTO(
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64, message = "角色名称不能超过 64 个字符")
        String name,
        @Size(max = 255, message = "角色描述不能超过 255 个字符")
        String description
) {
}
