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
import xyz.jiayuhan.userservice.model.request.account.ChangePasswordRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateProfileRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateStudentProfileRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateTeacherProfileRequest;
import xyz.jiayuhan.userservice.model.response.account.StudentProfileResponse;
import xyz.jiayuhan.userservice.model.response.account.TeacherProfileResponse;
import xyz.jiayuhan.userservice.model.response.account.UserProfileResponse;
import xyz.jiayuhan.userservice.service.RbacService;
import xyz.jiayuhan.userservice.service.ProfileService;
import xyz.jiayuhan.userservice.service.UserAccountService;
import xyz.jiayuhan.userservice.security.RoleCodes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** 当前登录用户的资料查询、自助编辑和安全改密实现。 */
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserDataSupport userDataSupport;
    private final RbacService rbacService;
    private final ProfileService profileService;
    private final UserEventOutboxService eventOutboxService;

    @Override
    public UserProfileResponse currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return toProfile(requireUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
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
                .set(User::getUpdatedAt, OffsetDateTime.now(ZoneOffset.UTC)));
        appendSelfAudit(userId, UserAuditActions.SELF_PROFILE_UPDATED, "用户修改个人资料");
        eventOutboxService.appendLifecycle(
                userId, "PROFILE_UPDATED", requireUser(userId).getStatus(), rbacService.roleCodes(userId));
        return toProfile(requireUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = requireUser(userId);
        if (user.getPasswordHash() == null) {
            throw new BusinessException(ResultCode.PASSWORD_NOT_SET);
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        User update = new User();
        update.setId(userId);
        update.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        update.setPasswordChangedAt(OffsetDateTime.now(ZoneOffset.UTC));
        userMapper.updateById(update);
        appendSelfAudit(userId, UserAuditActions.SELF_PASSWORD_CHANGED, "用户自助修改密码");
        eventOutboxService.appendLifecycle(
                userId, "PASSWORD_CHANGED", user.getStatus(), rbacService.roleCodes(userId));

        // 密码变化属于高风险操作，注销该账号的全部现有会话。
        StpUtil.kickout(userId);
    }

    @Override
    public StudentProfileResponse currentStudentProfile() {
        return profileService.studentProfile(StpUtil.getLoginIdAsLong());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentProfileResponse updateStudentProfile(UpdateStudentProfileRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        StudentProfileResponse profile = profileService.updateOwnStudentProfile(userId, request);
        eventOutboxService.appendLifecycle(
                userId, "STUDENT_PROFILE_UPDATED", requireUser(userId).getStatus(), rbacService.roleCodes(userId));
        return profile;
    }

    @Override
    public TeacherProfileResponse currentTeacherProfile() {
        return profileService.teacherProfile(StpUtil.getLoginIdAsLong());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeacherProfileResponse updateTeacherProfile(UpdateTeacherProfileRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        TeacherProfileResponse profile = profileService.updateOwnTeacherProfile(userId, request);
        eventOutboxService.appendLifecycle(
                userId, "TEACHER_PROFILE_UPDATED", requireUser(userId).getStatus(), rbacService.roleCodes(userId));
        return profile;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserProfileResponse toProfile(User user) {
        List<String> roles = rbacService.roleCodes(user.getId());
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getStatus(),
                roles,
                rbacService.permissionCodes(user.getId()),
                user.getWechatOpenid() != null || user.getWechatWebOpenid() != null,
                user.getPasswordHash() != null,
                roles.contains(RoleCodes.STUDENT) ? profileService.studentProfile(user.getId()) : null,
                roles.contains(RoleCodes.TEACHER) ? profileService.teacherProfile(user.getId()) : null,
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
