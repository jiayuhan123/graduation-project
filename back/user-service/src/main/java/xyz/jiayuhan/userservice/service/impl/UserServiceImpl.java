package xyz.jiayuhan.userservice.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userservice.entity.User;
import xyz.jiayuhan.userservice.mapper.UserMapper;
import xyz.jiayuhan.userservice.model.dto.LoginDTO;
import xyz.jiayuhan.userservice.model.dto.RegisterDTO;
import xyz.jiayuhan.userservice.model.vo.LoginVO;
import xyz.jiayuhan.userservice.model.vo.RegisterVO;
import xyz.jiayuhan.userservice.service.UserService;

import java.time.LocalDateTime;

/**
 * 用户账号服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * 基础框架的默认登录角色，后续可按新业务替换为持久化角色。
     */
    private static final String DEFAULT_ROLE = "user";

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 1. 按用户名或手机号查询用户账号
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.username())
                .or()
                .eq(User::getPhone, loginDTO.username()));

        // 2. 账号不存在或密码错误统一返回同一错误码，避免账号枚举
        if (user == null || !passwordEncoder.matches(loginDTO.password(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 3. 校验账号状态
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 4. Sa-Token 登录，底层自动生成 Token 并写入 Redis
        StpUtil.login(user.getId());

        // 5. 封装登录结果
        return new LoginVO(
                StpUtil.getTokenValue(),
                StpUtil.getTokenName(),
                user.getId(),
                user.getUsername(),
                DEFAULT_ROLE,
                LocalDateTime.now().plusSeconds(StpUtil.getTokenTimeout())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterVO register(RegisterDTO registerDTO) {
        // 1. 用户名唯一性校验
        boolean usernameExists = userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.username()));
        if (usernameExists) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        // 2. 手机号唯一性校验
        boolean phoneExists = userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, registerDTO.phone()));
        if (phoneExists) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }

        // 3. 落库（密码 BCrypt 加密存储）
        User user = new User();
        user.setUsername(registerDTO.username());
        user.setPassword(passwordEncoder.encode(registerDTO.password()));
        user.setRealName(registerDTO.realName());
        user.setPhone(registerDTO.phone());
        user.setAvatar("");
        user.setStatus(1);
        userMapper.insert(user);

        log.info("用户注册成功：userId={}, username={}", user.getId(), user.getUsername());
        return new RegisterVO(user.getId(), user.getUsername());
    }
}
