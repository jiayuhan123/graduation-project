package xyz.jiayuhan.userservice.model.request.admin;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 完整替换角色权限请求。
 *
 * @param permissionIds 最终权限 ID 集合；空数组表示清空全部权限
 */
public record AssignPermissionsRequest(
        @NotNull(message = "权限列表不能为空")
        List<@NotNull(message = "权限 ID 不能为空") Long> permissionIds
) {
}
