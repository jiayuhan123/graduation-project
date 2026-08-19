package xyz.jiayuhan.userservice.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userservice.model.dto.LoginDTO;
import xyz.jiayuhan.userservice.model.dto.RegisterDTO;
import xyz.jiayuhan.userservice.model.vo.LoginVO;
import xyz.jiayuhan.userservice.model.vo.RegisterVO;
import xyz.jiayuhan.userservice.model.vo.SessionVO;
import xyz.jiayuhan.userservice.service.AuthService;

/** 注册与认证接口。 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录", description = "支持用户名、手机号或邮箱登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO request, HttpServletRequest servletRequest) {
        return Result.success("登录成功", authService.login(request, servletRequest.getRemoteAddr()));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<RegisterVO> register(@Valid @RequestBody RegisterDTO request) {
        return Result.success("注册成功", authService.register(request));
    }

    @SaCheckLogin
    @Operation(summary = "退出当前会话")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @SaCheckLogin
    @Operation(summary = "获取当前会话、角色和权限")
    @GetMapping("/session")
    public Result<SessionVO> currentSession() {
        return Result.success(authService.currentSession());
    }
}

