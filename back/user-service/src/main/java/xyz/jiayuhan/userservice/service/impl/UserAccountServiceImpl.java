package xyz.jiayuhan.userservice.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.userservice.entity.User;
import xyz.jiayuhan.userservice.event.UserAuditActions;
import xyz.jiayuhan.userservice.event.UserEventOutboxService;
import xyz.jiayuhan.userservice.mapper.UserMapper;
import xyz.jiayuhan.userservice.model.dto.ChangePasswordDTO;
import xyz.jiayuhan.userservice.model.dto.UpdateProfileDTO;
import xyz.jiayuhan.userservice.model.vo.UserProfileVO;
import xyz.jiayuhan.userservice.service.RbacService;
import xyz.jiayuhan.userservice.service.UserAccountService;

import java.time.LocalDateTime;

/** 当前登录用户的资料查询、自助编辑和安全改密实现。 */
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserDataSupport userDataSupport;
    private final RbacService rbacService;
    private final UserEventOutboxService eventOutboxService;

    @Override
    public UserProfileVO currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return toProfile(requireUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfile(UpdateProfileDTO request) {
        Long userId = StpUtil.getLoginIdAsLong();
        requireUser(userId);

        String phone = userDataSupport.trimToNull(request.phone());
        String email = userDataSupport.normalizeEmail(request.email());
        userDataSupport.checkUnique(null, phone, email, userId);

        // 显式 set 允许用户用空白值清空可选字段；实体的非空更新策略无法表达这一语义。
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getNickname, userDataSupport.trimToNull(request.nickname()))
                .set(User::getRealName, userDataSupport.trimToNull(request.realName()))
                .set(User::getEmail, email)
                .set(User::getPhone, phone)
                .set(User::getAvatarUrl, userDataSupport.trimToNull(request.avatarUrl()))
                .set(User::getUpdatedAt, LocalDateTime.now()));
        appendSelfAudit(userId, UserAuditActions.SELF_PROFILE_UPDATED, "用户修改个人资料");
        return toProfile(requireUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO request) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        User update = new User();
        update.setId(userId);
        update.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        update.setPasswordChangedAt(LocalDateTime.now());
        userMapper.updateById(update);
        appendSelfAudit(userId, UserAuditActions.SELF_PASSWORD_CHANGED, "用户自助修改密码");

        // 密码变化属于高风险操作，注销该账号的全部现有会话。
        StpUtil.kickout(userId);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserProfileVO toProfile(User user) {
        return new UserProfileVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getStatus(),
                rbacService.roleCodes(user.getId()),
                rbacService.permissionCodes(user.getId()),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
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
}
