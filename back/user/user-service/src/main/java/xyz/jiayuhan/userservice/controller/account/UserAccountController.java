package xyz.jiayuhan.userservice.controller.account;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userservice.model.request.account.ChangePasswordRequest;
import xyz.jiayuhan.userservice.model.request.account.InitializePasswordRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateProfileRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateStudentProfileRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateTeacherProfileRequest;
import xyz.jiayuhan.userservice.model.request.account.WechatCodeRequest;
import xyz.jiayuhan.userservice.model.response.account.StudentProfileResponse;
import xyz.jiayuhan.userservice.model.response.account.TeacherProfileResponse;
import xyz.jiayuhan.userservice.model.response.account.UserProfileResponse;
import xyz.jiayuhan.userservice.service.AuthService;
import xyz.jiayuhan.userservice.service.UserAccountService;

/** 当前登录用户的个人账户接口。 */
@Tag(name = "个人账户")
@SaCheckLogin
@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final AuthService authService;

    @Operation(summary = "获取当前用户资料")
    @GetMapping
    public Result<UserProfileResponse> currentUser() {
        return Result.success(userAccountService.currentUser());
    }

    @Operation(summary = "修改当前用户资料")
    @PutMapping
    public Result<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success("资料更新成功", userAccountService.updateProfile(request));
    }

    @Operation(summary = "修改密码", description = "成功后该账号的已有会话全部失效")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userAccountService.changePassword(request);
        return Result.success();
    }

    @Operation(summary = "微信账号首次设置密码", description = "成功后全部已有会话失效")
    @PutMapping("/password/initialize")
    public Result<Void> initializePassword(@Valid @RequestBody InitializePasswordRequest request) {
        authService.initializePassword(request.newPassword());
        return Result.success();
    }

    @Operation(summary = "绑定微信小程序身份")
    @PostMapping("/wechat")
    public Result<Void> bindWechat(@Valid @RequestBody WechatCodeRequest request) {
        authService.bindWechat(request.code());
        return Result.success();
    }

    @Operation(summary = "解绑微信小程序身份")
    @DeleteMapping("/wechat")
    public Result<Void> unbindWechat() {
        authService.unbindWechat();
        return Result.success();
    }

    @Operation(summary = "查询当前学生资料")
    @SaCheckRole("student")
    @GetMapping("/student-profile")
    public Result<StudentProfileResponse> studentProfile() {
        return Result.success(userAccountService.currentStudentProfile());
    }

    @Operation(summary = "修改当前学生的年级和学期")
    @SaCheckRole("student")
    @PutMapping("/student-profile")
    public Result<StudentProfileResponse> updateStudentProfile(
            @Valid @RequestBody UpdateStudentProfileRequest request) {
        return Result.success("学生资料更新成功", userAccountService.updateStudentProfile(request));
    }

    @Operation(summary = "查询当前教师资料")
    @SaCheckRole("teacher")
    @GetMapping("/teacher-profile")
    public Result<TeacherProfileResponse> teacherProfile() {
        return Result.success(userAccountService.currentTeacherProfile());
    }

    @Operation(summary = "修改当前教师公开资料")
    @SaCheckRole("teacher")
    @PutMapping("/teacher-profile")
    public Result<TeacherProfileResponse> updateTeacherProfile(
            @Valid @RequestBody UpdateTeacherProfileRequest request) {
        return Result.success("教师资料更新成功", userAccountService.updateTeacherProfile(request));
    }
}
