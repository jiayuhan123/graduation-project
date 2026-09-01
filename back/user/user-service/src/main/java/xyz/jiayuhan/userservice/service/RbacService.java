package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userservice.entity.Role;

import java.util.List;

/**
 * RBAC 查询与关联维护服务。
 *
 * <p>关联修改采用“完整替换”语义，而非增量追加，便于调用方明确最终授权状态。</p>
 */
public interface RbacService {

    /**
     * 查询用户当前所有已启用角色编码。
     *
     * @param userId 用户 ID
     * @return 角色编码列表
     */
    List<String> roleCodes(Long userId);

    /**
     * 查询用户通过已启用角色获得的去重权限编码。
     *
     * @param userId 用户 ID
     * @return 权限编码列表
     */
    List<String> permissionCodes(Long userId);

    /**
     * 查询用户当前所有已启用角色实体。
     *
     * @param userId 用户 ID
     * @return 角色列表
     */
    List<Role> roles(Long userId);

    /** 查询并校验给定角色 ID 对应的已启用角色编码。 */
    List<String> roleCodesByIds(List<Long> roleIds);

    /**
     * 为新注册用户分配系统默认角色。
     *
     * @param userId 用户 ID
     */
    void assignDefaultRole(Long userId);

    /**
     * 使用给定集合完整替换用户角色；仅允许分配存在且已启用的角色。
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 集合
     */
    void replaceUserRoles(Long userId, List<Long> roleIds);

    /**
     * 使用给定集合完整替换角色权限；空集合表示清空权限。
     *
     * @param roleId 角色 ID
     * @param permissionIds 权限 ID 集合
     */
    void replaceRolePermissions(Long roleId, List<Long> permissionIds);
}
