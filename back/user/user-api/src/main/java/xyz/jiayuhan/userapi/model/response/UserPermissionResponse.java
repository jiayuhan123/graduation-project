package xyz.jiayuhan.userapi.model.response;

/** 供业务服务校验用户状态和权限的最小响应。 */
public record UserPermissionResponse(
        Long userId,
        String permissionCode,
        boolean exists,
        boolean active,
        boolean granted
) {
}
