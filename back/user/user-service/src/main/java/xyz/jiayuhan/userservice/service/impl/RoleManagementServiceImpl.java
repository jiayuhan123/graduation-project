package xyz.jiayuhan.userservice.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.userservice.entity.Permission;
import xyz.jiayuhan.userservice.entity.Role;
import xyz.jiayuhan.userservice.entity.UserRole;
import xyz.jiayuhan.userservice.event.UserAuditActions;
import xyz.jiayuhan.userservice.event.UserEventOutboxService;
import xyz.jiayuhan.userservice.mapper.PermissionMapper;
import xyz.jiayuhan.userservice.mapper.RoleMapper;
import xyz.jiayuhan.userservice.mapper.UserRoleMapper;
import xyz.jiayuhan.userservice.model.request.admin.CreateRoleRequest;
import xyz.jiayuhan.userservice.model.request.admin.UpdateRoleRequest;
import xyz.jiayuhan.userservice.model.response.admin.PermissionResponse;
import xyz.jiayuhan.userservice.model.response.admin.RoleResponse;
import xyz.jiayuhan.userservice.service.RbacService;
import xyz.jiayuhan.userservice.service.RoleManagementService;

import java.util.List;

/**
 * 角色与权限字典管理实现，集中执行角色编码唯一性、内置角色保护和在用检查。
 */
@Service
@RequiredArgsConstructor
public class RoleManagementServiceImpl implements RoleManagementService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RbacService rbacService;
    private final UserEventOutboxService eventOutboxService;

    @Override
    public List<RoleResponse> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId))
                .stream().map(this::toRole).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleMapper.exists(new LambdaQueryWrapper<Role>().eq(Role::getCode, request.code()))) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS);
        }
        Role role = new Role();
        role.setCode(request.code());
        role.setName(request.name().trim());
        role.setDescription(trimToNull(request.description()));
        role.setStatus(1);
        role.setBuiltIn(false);
        roleMapper.insert(role);
        appendAudit(UserAuditActions.ROLE_CREATED, role.getId(), "code=" + role.getCode());
        return toRole(roleMapper.selectById(role.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleResponse updateRole(Long roleId, UpdateRoleRequest request) {
        Role role = requireRole(roleId);
        role.setName(request.name().trim());
        role.setDescription(trimToNull(request.description()));
        roleMapper.updateById(role);
        appendAudit(UserAuditActions.ROLE_UPDATED, roleId, "修改角色名称和描述");
        return toRole(roleMapper.selectById(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoleStatus(Long roleId, Integer status) {
        Role role = requireRole(roleId);
        // 内置 student/teacher/admin 角色是系统授权链路的基础，不允许被禁用。
        protectBuiltIn(role);
        role.setStatus(status);
        roleMapper.updateById(role);
        appendAudit(UserAuditActions.ROLE_STATUS_CHANGED, roleId, "status=" + status);
        kickoutRoleUsers(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        Role role = requireRole(roleId);
        protectBuiltIn(role);
        rbacService.replaceRolePermissions(roleId, permissionIds);
        appendAudit(
                UserAuditActions.ROLE_PERMISSIONS_CHANGED,
                roleId,
                "permissionCount=" + permissionIds.size());
        kickoutRoleUsers(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        Role role = requireRole(roleId);
        protectBuiltIn(role);
        // 拒绝删除仍被用户引用的角色，避免用户在无感知情况下失去授权。
        if (userRoleMapper.exists(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId))) {
            throw new BusinessException(ResultCode.ROLE_IN_USE);
        }
        roleMapper.deleteById(roleId);
        appendAudit(UserAuditActions.ROLE_DELETED, roleId, "删除未使用的自定义角色");
    }

    @Override
    public List<PermissionResponse> listPermissions() {
        return permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getId))
                .stream().map(this::toPermission).toList();
    }

    private Role requireRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    private void protectBuiltIn(Role role) {
        if (Boolean.TRUE.equals(role.getBuiltIn())) {
            throw new BusinessException(ResultCode.BUILT_IN_ROLE_PROTECTED);
        }
    }

    private RoleResponse toRole(Role role) {
        List<PermissionResponse> permissions = permissionMapper.selectByRoleId(role.getId())
                .stream().map(this::toPermission).toList();
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getStatus(),
                role.getBuiltIn(),
                permissions,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    private PermissionResponse toPermission(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void appendAudit(String action, Long roleId, String detail) {
        eventOutboxService.appendAudit(
                MqTopology.AUDIT_OPERATION_ROUTING_KEY,
                StpUtil.getLoginIdAsLong(),
                action,
                "ROLE",
                roleId,
                detail);
    }

    private void kickoutRoleUsers(Long roleId) {
        userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId))
                .stream()
                .map(UserRole::getUserId)
                .distinct()
                .forEach(StpUtil::kickout);
    }
}
