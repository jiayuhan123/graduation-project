package xyz.jiayuhan.userservice.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 完整替换用户角色请求。
 *
 * @param roleIds 最终角色 ID 集合；用户至少保留一个已启用角色
 */
public record AssignRolesDTO(
        @NotEmpty(message = "用户至少需要一个角色")
        List<@NotNull(message = "角色 ID 不能为空") Long> roleIds
) {
}
