package xyz.jiayuhan.userservice.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userservice.model.request.admin.AssignPermissionsRequest;
import xyz.jiayuhan.userservice.model.request.admin.CreateRoleRequest;
import xyz.jiayuhan.userservice.model.request.admin.UpdateRoleRequest;
import xyz.jiayuhan.userservice.model.request.admin.UpdateStatusRequest;
import xyz.jiayuhan.userservice.model.response.admin.PermissionResponse;
import xyz.jiayuhan.userservice.model.response.admin.RoleResponse;
import xyz.jiayuhan.userservice.service.RoleManagementService;

import java.util.List;

/** 管理端 RBAC 接口。 */
@Tag(name = "角色权限管理")
@RestController
@RequestMapping("/api/user/admin")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleManagementService roleManagementService;

    @SaCheckPermission("system:role:manage")
    @Operation(summary = "查询全部角色及权限")
    @GetMapping("/roles")
    public Result<List<RoleResponse>> listRoles() {
        return Result.success(roleManagementService.listRoles());
    }

    @SaCheckPermission("system:role:manage")
    @Operation(summary = "创建自定义角色")
    @PostMapping("/roles")
    public Result<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return Result.success("角色创建成功", roleManagementService.createRole(request));
    }

    @SaCheckPermission("system:role:manage")
    @Operation(summary = "修改角色")
    @PutMapping("/roles/{roleId}")
    public Result<RoleResponse> updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRoleRequest request) {
        return Result.success("角色更新成功", roleManagementService.updateRole(roleId, request));
    }

    @SaCheckPermission("system:role:manage")
    @Operation(summary = "启用或禁用自定义角色")
    @PutMapping("/roles/{roleId}/status")
    public Result<Void> updateRoleStatus(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateStatusRequest request) {
        roleManagementService.updateRoleStatus(roleId, request.status());
        return Result.success();
    }

    @SaCheckPermission("system:role:manage")
    @Operation(summary = "替换自定义角色权限")
    @PutMapping("/roles/{roleId}/permissions")
    public Result<Void> assignPermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody AssignPermissionsRequest request) {
        roleManagementService.assignPermissions(roleId, request.permissionIds());
        return Result.success();
    }

    @SaCheckPermission("system:role:manage")
    @Operation(summary = "删除未使用的自定义角色")
    @DeleteMapping("/roles/{roleId}")
    public Result<Void> deleteRole(@PathVariable Long roleId) {
        roleManagementService.deleteRole(roleId);
        return Result.success();
    }

    @SaCheckPermission("system:role:manage")
    @Operation(summary = "查询权限字典")
    @GetMapping("/permissions")
    public Result<List<PermissionResponse>> listPermissions() {
        return Result.success(roleManagementService.listPermissions());
    }
}
