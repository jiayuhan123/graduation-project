package xyz.jiayuhan.userservice.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色及其权限详情。
 *
 * @param id 角色 ID
 * @param code 程序鉴权使用的稳定角色编码
 * @param name 角色名称
 * @param description 角色说明
 * @param status 状态：1 启用、0 禁用
 * @param builtIn 是否为受保护的系统内置角色
 * @param permissions 角色拥有的权限
 * @param createdAt 创建时间
 * @param updatedAt 最近更新时间
 */
public record RoleVO(
        Long id,
        String code,
        String name,
        String description,
        Integer status,
        Boolean builtIn,
        List<PermissionVO> permissions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
