package xyz.jiayuhan.userservice.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;
import xyz.jiayuhan.userservice.entity.User;
import xyz.jiayuhan.userservice.event.UserEventOutboxService;
import xyz.jiayuhan.userservice.mapper.UserMapper;
import xyz.jiayuhan.userservice.model.dto.LoginDTO;
import xyz.jiayuhan.userservice.model.dto.RegisterDTO;
import xyz.jiayuhan.userservice.model.vo.LoginVO;
import xyz.jiayuhan.userservice.model.vo.RegisterVO;
import xyz.jiayuhan.userservice.model.vo.SessionVO;
import xyz.jiayuhan.userservice.service.AuthService;
import xyz.jiayuhan.userservice.service.LoginAttemptService;
import xyz.jiayuhan.userservice.service.RbacService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务实现，负责账号规范化、凭证校验、登录防刷、会话创建与登录审计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserDataSupport userDataSupport;
    private final RbacService rbacService;
    private final LoginAttemptService loginAttemptService;
    private final UserEventOutboxService eventOutboxService;

    @Override
    public LoginVO login(LoginDTO request, String clientIp) {
        // 三种登录标识统一按小写查询，避免用户名和邮箱因大小写产生重复身份。
        String account = request.username().trim().toLowerCase(java.util.Locale.ROOT);
        loginAttemptService.checkAllowed(account);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(wrapper -> wrapper
                        .eq(User::getUsername, account)
                        .or().eq(User::getPhone, account)
                        .or().eq(User::getEmail, account)));

        // 账号不存在和密码错误使用同一响应，避免攻击者据此枚举已注册账号。
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(account);
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        loginAttemptService.clear(account);
        StpUtil.login(user.getId());

        // 登录审计只更新必要字段，避免把查询实体中的旧值整体写回并覆盖并发修改。
        LocalDateTime now = LocalDateTime.now();
        User loginAudit = new User();
        loginAudit.setId(user.getId());
        loginAudit.setLastLoginAt(now);
        loginAudit.setLastLoginIp(userDataSupport.trimToNull(clientIp));
        userMapper.updateById(loginAudit);

        List<String> roles = rbacService.roleCodes(user.getId());
        List<String> permissions = rbacService.permissionCodes(user.getId());
        long timeout = StpUtil.getTokenTimeout();
        log.info("用户登录成功：userId={}", user.getId());
        return new LoginVO(
                StpUtil.getTokenValue(),
                StpUtil.getTokenName(),
                user.getId(),
                user.getUsername(),
                primaryRole(roles),
                roles,
                permissions,
                timeout,
                expireAt(timeout)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterVO register(RegisterDTO request) {
        String username = userDataSupport.normalizeUsername(request.username());
        String phone = userDataSupport.trimToNull(request.phone());
        String email = userDataSupport.normalizeEmail(request.email());
        userDataSupport.checkUnique(username, phone, email, null);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(userDataSupport.trimToNull(request.nickname()));
        user.setRealName(userDataSupport.trimToNull(request.realName()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setAvatarUrl(null);
        user.setStatus(1);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setDeleted(false);
        user.setVersion(0);
        userMapper.insert(user);
        // 用户写入与默认角色分配处于同一事务，防止产生没有基础角色的半成品账号。
        rbacService.assignDefaultRole(user.getId());
        // 同事务写入发件箱；通知和审计服务随后异步消费，不延长注册接口主链路。
        eventOutboxService.append(
                MqTopology.USER_REGISTERED_ROUTING_KEY,
                UserRegisteredEvent.create(user.getId(), user.getUsername(), user.getEmail()));

        log.info("用户注册成功：userId={}, username={}", user.getId(), user.getUsername());
        return new RegisterVO(user.getId(), user.getUsername());
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public SessionVO currentSession() {
        Long userId = StpUtil.getLoginIdAsLong();
        long timeout = StpUtil.getTokenTimeout();
        return new SessionVO(
                userId,
                StpUtil.getTokenValue(),
                StpUtil.getTokenName(),
                rbacService.roleCodes(userId),
                rbacService.permissionCodes(userId),
                timeout,
                expireAt(timeout)
        );
    }

    private String primaryRole(List<String> roles) {
        // 仅用于兼容旧客户端的单 role 字段；服务端真实授权始终以 roles/permissions 为准。
        if (roles.contains("admin")) {
            return "admin";
        }
        return roles.isEmpty() ? "user" : roles.getFirst();
    }

    private LocalDateTime expireAt(long timeout) {
        // Sa-Token 使用负数表达长期有效等特殊状态，此时不存在可计算的绝对过期时间。
        return timeout < 0 ? null : LocalDateTime.now().plusSeconds(timeout);
    }
}
