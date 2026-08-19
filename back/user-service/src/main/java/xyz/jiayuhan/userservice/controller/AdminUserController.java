package xyz.jiayuhan.userservice.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.userservice.model.dto.AdminCreateUserDTO;
import xyz.jiayuhan.userservice.model.dto.AdminUpdateUserDTO;
import xyz.jiayuhan.userservice.model.dto.AssignRolesDTO;
import xyz.jiayuhan.userservice.model.dto.ResetPasswordDTO;
import xyz.jiayuhan.userservice.model.dto.UpdateStatusDTO;
import xyz.jiayuhan.userservice.model.vo.AdminUserVO;
import xyz.jiayuhan.userservice.service.AdminUserService;

/** 管理端用户维护接口。 */
@Tag(name = "用户管理")
@Validated
@RestController
@RequestMapping("/api/user/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @SaCheckPermission("system:user:list")
    @Operation(summary = "分页查询用户")
    @GetMapping
    public Result<PageResult<AdminUserVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return Result.success(adminUserService.page(page, size, keyword, status));
    }

    @SaCheckPermission("system:user:query")
    @Operation(summary = "查询用户详情")
    @GetMapping("/{userId}")
    public Result<AdminUserVO> detail(@PathVariable Long userId) {
        return Result.success(adminUserService.detail(userId));
    }

    @SaCheckPermission("system:user:create")
    @Operation(summary = "创建用户")
    @PostMapping
    public Result<AdminUserVO> create(@Valid @RequestBody AdminCreateUserDTO request) {
        return Result.success("用户创建成功", adminUserService.create(request));
    }

    @SaCheckPermission("system:user:update")
    @Operation(summary = "修改用户资料")
    @PutMapping("/{userId}")
    public Result<AdminUserVO> update(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserDTO request) {
        return Result.success("用户资料更新成功", adminUserService.update(userId, request));
    }

    @SaCheckPermission("system:user:status")
    @Operation(summary = "启用或禁用用户")
    @PutMapping("/{userId}/status")
    public Result<Void> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStatusDTO request) {
        adminUserService.updateStatus(userId, request.status());
        return Result.success();
    }

    @SaCheckPermission("system:user:role")
    @Operation(summary = "替换用户角色")
    @PutMapping("/{userId}/roles")
    public Result<Void> assignRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRolesDTO request) {
        adminUserService.assignRoles(userId, request.roleIds());
        return Result.success();
    }

    @SaCheckPermission("system:user:password-reset")
    @Operation(summary = "重置用户密码")
    @PutMapping("/{userId}/password")
    public Result<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody ResetPasswordDTO request) {
        adminUserService.resetPassword(userId, request.newPassword());
        return Result.success();
    }

    @SaCheckPermission("system:user:delete")
    @Operation(summary = "软删除用户")
    @DeleteMapping("/{userId}")
    public Result<Void> delete(@PathVariable Long userId) {
        adminUserService.delete(userId);
        return Result.success();
    }
}
