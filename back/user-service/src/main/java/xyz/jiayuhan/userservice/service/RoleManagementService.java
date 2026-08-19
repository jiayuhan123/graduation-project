package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userservice.model.dto.CreateRoleDTO;
import xyz.jiayuhan.userservice.model.dto.UpdateRoleDTO;
import xyz.jiayuhan.userservice.model.vo.PermissionVO;
import xyz.jiayuhan.userservice.model.vo.RoleVO;

import java.util.List;

/**
 * 角色与权限管理服务。
 *
 * <p>系统内置角色是基础授权模型的一部分，不允许禁用、改权或删除；自定义角色
 * 只有在未分配给任何用户时才能删除。</p>
 */
public interface RoleManagementService {

    /** @return 全部角色及各自权限 */
    List<RoleVO> listRoles();

    /**
     * 创建一个默认启用的自定义角色。
     *
     * @param request 创建参数
     * @return 新角色详情
     */
    RoleVO createRole(CreateRoleDTO request);

    /**
     * 修改角色名称和描述，角色编码保持不变。
     *
     * @param roleId 角色 ID
     * @param request 修改参数
     * @return 修改后的角色详情
     */
    RoleVO updateRole(Long roleId, UpdateRoleDTO request);

    /**
     * 启用或禁用自定义角色。
     *
     * @param roleId 角色 ID
     * @param status 1 启用、0 禁用
     */
    void updateRoleStatus(Long roleId, Integer status);

    /**
     * 完整替换自定义角色的权限集合。
     *
     * @param roleId 角色 ID
     * @param permissionIds 最终权限 ID 集合
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 删除一个未被用户使用的自定义角色。
     *
     * @param roleId 角色 ID
     */
    void deleteRole(Long roleId);

    /** @return 可分配的系统权限字典 */
    List<PermissionVO> listPermissions();
}
