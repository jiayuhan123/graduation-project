package xyz.jiayuhan.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userservice.model.dto.LoginDTO;
import xyz.jiayuhan.userservice.model.dto.RegisterDTO;
import xyz.jiayuhan.userservice.model.vo.LoginVO;
import xyz.jiayuhan.userservice.model.vo.RegisterVO;
import xyz.jiayuhan.userservice.service.UserService;

/**
 * 用户账号接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success("登录成功", userService.login(loginDTO));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<RegisterVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return Result.success("注册成功", userService.register(registerDTO));
    }
}
