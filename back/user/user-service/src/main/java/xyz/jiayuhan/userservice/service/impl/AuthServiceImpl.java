package xyz.jiayuhan.userservice.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import xyz.jiayuhan.userservice.event.UserAuditActions;
import xyz.jiayuhan.userservice.integration.wechat.WechatIdentityClient;
import xyz.jiayuhan.userservice.mapper.UserMapper;
import xyz.jiayuhan.userservice.model.request.auth.LoginRequest;
import xyz.jiayuhan.userservice.model.request.auth.RegisterRequest;
import xyz.jiayuhan.userservice.model.request.auth.WechatLoginRequest;
import xyz.jiayuhan.userservice.model.response.auth.LoginResponse;
import xyz.jiayuhan.userservice.model.response.auth.RegisterResponse;
import xyz.jiayuhan.userservice.model.response.auth.SessionResponse;
import xyz.jiayuhan.userservice.security.RoleCodes;
import xyz.jiayuhan.userservice.service.AuthService;
import xyz.jiayuhan.userservice.service.LoginAttemptService;
import xyz.jiayuhan.userservice.service.ProfileService;
import xyz.jiayuhan.userservice.service.RbacService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 账号注册、密码/微信登录、会话和登录凭证维护实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserDataSupport userDataSupport;
    private final RbacService rbacService;
    private final ProfileService profileService;
    private final LoginAttemptService loginAttemptService;
    private final UserEventOutboxService eventOutboxService;
    private final WechatIdentityClient wechatIdentityClient;

    @Override
    public LoginResponse login(LoginRequest request, String clientIp) {
        String account = request.username().trim().toLowerCase(Locale.ROOT);
        loginAttemptService.checkAllowed(account);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(wrapper -> wrapper
                        .eq(User::getUsername, account)
                        .or().eq(User::getPhone, account)
                        .or().eq(User::getEmail, account)));
        if (user == null
                || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(account);
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        ensureActive(user);
        loginAttemptService.clear(account);
        return createSession(user, clientIp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse loginWithWechat(WechatLoginRequest request, String clientIp) {
        String openId = wechatIdentityClient.exchangeOpenId(request.code());
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getWechatOpenid, openId));
        if (user == null) {
            if (request.gradeLevel() == null || request.semester() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "首次微信登录必须填写年级和学期");
            }
            user = createWechatStudent(openId, request);
        }
        ensureActive(user);
        return createSession(user, clientIp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse register(RegisterRequest request) {
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
        user.setStatus(1);
        user.setPasswordChangedAt(now());
        user.setDeleted(false);
        user.setVersion(0);
        userMapper.insert(user);

        profileService.upsertStudentProfile(
                user.getId(), null, request.gradeLevel(), request.semester());
        rbacService.assignDefaultRole(user.getId());
        appendRegistered(user);
        log.info("用户注册成功：userId={}", user.getId());
        return new RegisterResponse(user.getId(), user.getUsername());
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public void logoutAll() {
        StpUtil.logout(StpUtil.getLoginIdAsLong());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindWechat(String code) {
        Long userId = StpUtil.getLoginIdAsLong();
        User current = requireUser(userId);
        String openId = wechatIdentityClient.exchangeOpenId(code);
        User occupied = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getWechatOpenid, openId));
        if (occupied != null && !occupied.getId().equals(userId)) {
            throw new BusinessException(ResultCode.WECHAT_ALREADY_BOUND);
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getWechatOpenid, openId)
                .set(User::getUpdatedAt, now()));
        eventOutboxService.appendLifecycle(
                userId, "WECHAT_BOUND", current.getStatus(), rbacService.roleCodes(userId));
        appendSelfAudit(userId, UserAuditActions.SELF_WECHAT_BOUND, "用户绑定微信身份");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindWechat() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = requireUser(userId);
        if (user.getPasswordHash() == null) {
            throw new BusinessException(ResultCode.LOGIN_CREDENTIAL_REQUIRED);
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getWechatOpenid, null)
                .set(User::getUpdatedAt, now()));
        eventOutboxService.appendLifecycle(
                userId, "WECHAT_UNBOUND", user.getStatus(), rbacService.roleCodes(userId));
        appendSelfAudit(userId, UserAuditActions.SELF_WECHAT_UNBOUND, "用户解绑微信身份");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializePassword(String newPassword) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = requireUser(userId);
        if (user.getPasswordHash() != null) {
            throw new BusinessException(ResultCode.PASSWORD_ALREADY_SET);
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(newPassword))
                .set(User::getPasswordChangedAt, now())
                .set(User::getUpdatedAt, now()));
        eventOutboxService.appendLifecycle(
                userId, "PASSWORD_INITIALIZED", user.getStatus(), rbacService.roleCodes(userId));
        appendSelfAudit(userId, UserAuditActions.SELF_PASSWORD_INITIALIZED, "微信账号首次设置密码");
        StpUtil.kickout(userId);
    }

    @Override
    public SessionResponse currentSession() {
        Long userId = StpUtil.getLoginIdAsLong();
        long timeout = StpUtil.getTokenTimeout();
        return new SessionResponse(
                userId,
                StpUtil.getTokenValue(),
                StpUtil.getTokenName(),
                rbacService.roleCodes(userId),
                rbacService.permissionCodes(userId),
                timeout,
                expireAt(timeout));
    }

    private User createWechatStudent(String openId, WechatLoginRequest request) {
        User user = new User();
        user.setUsername(generateWechatUsername());
        user.setWechatOpenid(openId);
        user.setNickname(userDataSupport.trimToNull(request.nickname()));
        user.setRealName(userDataSupport.trimToNull(request.realName()));
        user.setStatus(1);
        user.setDeleted(false);
        user.setVersion(0);
        userMapper.insert(user);
        profileService.upsertStudentProfile(
                user.getId(), null, request.gradeLevel(), request.semester());
        rbacService.assignDefaultRole(user.getId());
        appendRegistered(user);
        return user;
    }

    private String generateWechatUsername() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String username = "wx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            if (!userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, username))) {
                return username;
            }
        }
        throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "无法生成微信账号标识");
    }

    private LoginResponse createSession(User user, String clientIp) {
        StpUtil.login(user.getId());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLastLoginAt, now())
                .set(User::getLastLoginIp, userDataSupport.trimToNull(clientIp))
                .set(User::getUpdatedAt, now()));
        List<String> roles = rbacService.roleCodes(user.getId());
        List<String> permissions = rbacService.permissionCodes(user.getId());
        long timeout = StpUtil.getTokenTimeout();
        return new LoginResponse(
                StpUtil.getTokenValue(),
                StpUtil.getTokenName(),
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                primaryRole(roles),
                roles,
                permissions,
                timeout,
                expireAt(timeout));
    }

    private void appendRegistered(User user) {
        eventOutboxService.append(
                MqTopology.USER_REGISTERED_ROUTING_KEY,
                UserRegisteredEvent.create(user.getId(), user.getUsername(), user.getEmail()));
    }

    private void appendSelfAudit(Long userId, String action, String detail) {
        eventOutboxService.appendAudit(
                MqTopology.AUDIT_OPERATION_ROUTING_KEY,
                userId,
                action,
                "USER",
                userId,
                detail);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void ensureActive(User user) {
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
    }

    private String primaryRole(List<String> roles) {
        if (roles.contains(RoleCodes.ADMIN)) {
            return RoleCodes.ADMIN;
        }
        if (roles.contains(RoleCodes.TEACHER)) {
            return RoleCodes.TEACHER;
        }
        return roles.isEmpty() ? RoleCodes.STUDENT : roles.getFirst();
    }

    private OffsetDateTime expireAt(long timeout) {
        return timeout < 0 ? null : now().plusSeconds(timeout);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
