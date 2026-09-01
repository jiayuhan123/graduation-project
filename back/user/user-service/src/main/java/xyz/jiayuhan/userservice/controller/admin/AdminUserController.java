package xyz.jiayuhan.userservice.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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
import xyz.jiayuhan.userservice.model.request.admin.AdminCreateUserRequest;
import xyz.jiayuhan.userservice.model.request.admin.AdminUpdateUserRequest;
import xyz.jiayuhan.userservice.model.request.admin.AdminStudentProfileRequest;
import xyz.jiayuhan.userservice.model.request.admin.AdminTeacherProfileRequest;
import xyz.jiayuhan.userservice.model.request.admin.AssignRolesRequest;
import xyz.jiayuhan.userservice.model.request.admin.ResetPasswordRequest;
import xyz.jiayuhan.userservice.model.request.admin.UpdateStatusRequest;
import xyz.jiayuhan.userservice.model.response.admin.AdminUserResponse;
import xyz.jiayuhan.userservice.service.AdminUserService;

/** 管理端用户维护接口。 */
@Tag(name = "用户管理")
@Validated
@RestController
@RequestMapping("/api/user/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @SaCheckPermission("system:user:manage")
    @Operation(summary = "分页查询用户")
    @GetMapping
    public Result<PageResult<AdminUserResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status,
            @RequestParam(required = false)
            @Pattern(regexp = "^[a-z][a-z0-9_-]{1,63}$", message = "角色编码格式不正确")
            String roleCode) {
        return Result.success(adminUserService.page(page, size, keyword, status, roleCode));
    }

    @SaCheckPermission("system:user:manage")
    @Operation(summary = "查询用户详情")
    @GetMapping("/{userId}")
    public Result<AdminUserResponse> detail(@PathVariable Long userId) {
        return Result.success(adminUserService.detail(userId));
    }

    @SaCheckPermission("system:user:manage")
    @Operation(summary = "创建用户")
    @PostMapping
    public Result<AdminUserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return Result.success("用户创建成功", adminUserService.create(request));
    }

    @SaCheckPermission("system:user:manage")
    @Operation(summary = "修改用户资料")
    @PutMapping("/{userId}")
    public Result<AdminUserResponse> update(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return Result.success("用户资料更新成功", adminUserService.update(userId, request));
    }

    @SaCheckPermission("system:user:manage")
    @Operation(summary = "启用或禁用用户")
    @PutMapping("/{userId}/status")
    public Result<Void> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStatusRequest request) {
        adminUserService.updateStatus(userId, request.status());
        return Result.success();
    }

    @SaCheckPermission("system:user:manage")
    @Operation(summary = "替换用户角色")
    @PutMapping("/{userId}/roles")
    public Result<Void> assignRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRolesRequest request) {
        adminUserService.assignRoles(userId, request.roleIds());
        return Result.success();
    }

    @SaCheckPermission("system:user:manage")
    @Operation(summary = "重置用户密码")
    @PutMapping("/{userId}/password")
    public Result<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody ResetPasswordRequest request) {
        adminUserService.resetPassword(userId, request.newPassword());
        return Result.success();
    }

    @SaCheckPermission("system:user:manage")
    @Operation(summary = "软删除用户")
    @DeleteMapping("/{userId}")
    public Result<Void> delete(@PathVariable Long userId) {
        adminUserService.delete(userId);
        return Result.success();
    }

    @SaCheckPermission("system:user:manage")
    @Operation(summary = "创建或修改学生资料")
    @PutMapping("/{userId}/student-profile")
    public Result<AdminUserResponse> upsertStudentProfile(
            @PathVariable Long userId,
            @Valid @RequestBody AdminStudentProfileRequest request) {
        return Result.success("学生资料更新成功", adminUserService.upsertStudentProfile(userId, request));
    }

    @SaCheckPermission("system:user:manage")
    @Operation(summary = "创建或修改教师资料")
    @PutMapping("/{userId}/teacher-profile")
    public Result<AdminUserResponse> upsertTeacherProfile(
            @PathVariable Long userId,
            @Valid @RequestBody AdminTeacherProfileRequest request) {
        return Result.success("教师资料更新成功", adminUserService.upsertTeacherProfile(userId, request));
    }
}
