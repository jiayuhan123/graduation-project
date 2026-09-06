package xyz.jiayuhan.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userservice.entity.Permission;
import xyz.jiayuhan.userservice.entity.Role;
import xyz.jiayuhan.userservice.entity.RolePermission;
import xyz.jiayuhan.userservice.entity.UserRole;
import xyz.jiayuhan.userservice.mapper.PermissionMapper;
import xyz.jiayuhan.userservice.mapper.RoleMapper;
import xyz.jiayuhan.userservice.mapper.RolePermissionMapper;
import xyz.jiayuhan.userservice.mapper.UserRoleMapper;
import xyz.jiayuhan.userservice.service.RbacService;
import xyz.jiayuhan.userservice.security.RoleCodes;

import java.util.List;

/**
 * RBAC 查询与关联维护实现。
 *
 * <p>替换关联前先完整校验输入 ID，校验失败时不会提前删除已有授权。</p>
 */
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private static final String DEFAULT_ROLE_CODE = RoleCodes.STUDENT;

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<String> roleCodes(Long userId) {
        return roleMapper.selectCodesByUserId(userId);
    }

    @Override
    public List<String> permissionCodes(Long userId) {
        return permissionMapper.selectCodesByUserId(userId);
    }

    @Override
    public List<Role> roles(Long userId) {
        return roleMapper.selectEnabledByUserId(userId);
    }

    @Override
    public List<String> roleCodesByIds(List<Long> roleIds) {
        List<Long> distinctIds = roleIds.stream().distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户至少需要一个角色");
        }
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getId, distinctIds)
                .eq(Role::getStatus, 1));
        if (roles.size() != distinctIds.size()) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        return roles.stream().map(Role::getCode).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDefaultRole(Long userId) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, DEFAULT_ROLE_CODE)
                .eq(Role::getStatus, 1));
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND, "默认用户角色不存在");
        }
        UserRole relation = new UserRole();
        relation.setUserId(userId);
        relation.setRoleId(role.getId());
        userRoleMapper.insert(relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceUserRoles(Long userId, List<Long> roleIds) {
        // 去重后再计数校验，避免重复 ID 被误判为角色不存在。
        List<Long> distinctIds = roleIds.stream().distinct().toList();
        roleCodesByIds(distinctIds);

        // 先校验后替换；整个方法受事务保护，不会暴露角色被临时清空的中间状态。
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        distinctIds.forEach(roleId -> {
            UserRole relation = new UserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
        List<Long> distinctIds = permissionIds.stream().distinct().toList();
        if (!distinctIds.isEmpty()) {
            long count = permissionMapper.selectCount(
                    new LambdaQueryWrapper<Permission>().in(Permission::getId, distinctIds));
            if (count != distinctIds.size()) {
                throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
            }
        }

        // 空集合也是有效的最终状态，表示显式移除该角色的全部权限。
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));
        distinctIds.forEach(permissionId -> {
            RolePermission relation = new RolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        });
    }
}
