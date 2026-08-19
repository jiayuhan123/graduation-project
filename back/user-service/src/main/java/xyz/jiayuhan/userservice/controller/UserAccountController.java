package xyz.jiayuhan.userservice.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userservice.model.dto.ChangePasswordDTO;
import xyz.jiayuhan.userservice.model.dto.UpdateProfileDTO;
import xyz.jiayuhan.userservice.model.vo.UserProfileVO;
import xyz.jiayuhan.userservice.service.UserAccountService;

/** 当前登录用户的个人账户接口。 */
@Tag(name = "个人账户")
@SaCheckLogin
@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @Operation(summary = "获取当前用户资料")
    @GetMapping
    public Result<UserProfileVO> currentUser() {
        return Result.success(userAccountService.currentUser());
    }

    @Operation(summary = "修改当前用户资料")
    @PutMapping
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileDTO request) {
        return Result.success("资料更新成功", userAccountService.updateProfile(request));
    }

    @Operation(summary = "修改密码", description = "成功后该账号的已有会话全部失效")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO request) {
        userAccountService.changePassword(request);
        return Result.success();
    }
}

