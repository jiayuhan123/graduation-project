package xyz.jiayuhan.userservice.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;
import xyz.jiayuhan.userservice.entity.Role;
import xyz.jiayuhan.userservice.entity.User;
import xyz.jiayuhan.userservice.entity.UserRole;
import xyz.jiayuhan.userservice.event.UserAuditActions;
import xyz.jiayuhan.userservice.event.UserEventOutboxService;
import xyz.jiayuhan.userservice.mapper.UserMapper;
import xyz.jiayuhan.userservice.mapper.UserRoleMapper;
import xyz.jiayuhan.userservice.model.request.admin.AdminCreateUserRequest;
import xyz.jiayuhan.userservice.model.request.admin.AdminStudentProfileRequest;
import xyz.jiayuhan.userservice.model.request.admin.AdminTeacherProfileRequest;
import xyz.jiayuhan.userservice.model.request.admin.AdminUpdateUserRequest;
import xyz.jiayuhan.userservice.model.response.admin.AdminUserResponse;
import xyz.jiayuhan.userservice.model.response.admin.RoleSummaryResponse;
import xyz.jiayuhan.userservice.service.AdminUserService;
import xyz.jiayuhan.userservice.service.ProfileService;
import xyz.jiayuhan.userservice.service.RbacService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** 管理端用户、角色归属和角色专有资料维护实现。 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserDataSupport userDataSupport;
    private final RbacService rbacService;
    private final ProfileService profileService;
    private final UserRoleMapper userRoleMapper;
    private final UserEventOutboxService eventOutboxService;

    @Override
    public PageResult<AdminUserResponse> page(
            long page,
            long size,
            String keyword,
            Integer status,
            String roleCode) {
        String normalizedRole = userDataSupport.trimToNull(roleCode);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(status != null, User::getStatus, status)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword)
                        .or().like(User::getRealName, keyword)
                        .or().like(User::getEmail, keyword)
                        .or().like(User::getPhone, keyword))
                .exists(normalizedRole != null, """
                        SELECT 1
                        FROM sys_user_role ur
                        JOIN sys_role r ON r.id = ur.role_id
                        WHERE ur.user_id = t_user.id AND r.code = {0} AND r.status = 1
                        """, normalizedRole)
                .orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                result.getRecords().stream().map(this::toAdminUser).toList());
    }

    @Override
    public AdminUserResponse detail(Long userId) {
        return toAdminUser(requireUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse create(AdminCreateUserRequest request) {
        List<String> roleCodes = rbacService.roleCodesByIds(request.roleIds());
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
        user.setStatus(request.status() == null ? 1 : request.status());
        user.setPasswordChangedAt(now());
        user.setDeleted(false);
        user.setVersion(0);
        userMapper.insert(user);

        if (request.studentProfile() != null) {
            AdminStudentProfileRequest profile = request.studentProfile();
            profileService.upsertStudentProfile(
                    user.getId(), profile.studentNo(), profile.gradeLevel(), profile.semester());
        }
        if (request.teacherProfile() != null) {
            AdminTeacherProfileRequest profile = request.teacherProfile();
            profileService.upsertTeacherProfile(
                    user.getId(), profile.teacherNo(), profile.title(), profile.introduction());
        }
        profileService.validateRoleProfiles(user.getId(), roleCodes);
        rbacService.replaceUserRoles(user.getId(), request.roleIds());

        eventOutboxService.append(
                MqTopology.USER_REGISTERED_ROUTING_KEY,
                UserRegisteredEvent.create(user.getId(), user.getUsername(), user.getEmail()));
        appendAudit(UserAuditActions.USER_CREATED, user.getId(), "status=" + user.getStatus());
        return detail(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse update(Long userId, AdminUpdateUserRequest request) {
        requireUser(userId);
        String phone = userDataSupport.trimToNull(request.phone());
        String email = userDataSupport.normalizeEmail(request.email());
        userDataSupport.checkUnique(null, phone, email, userId);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getNickname, userDataSupport.trimToNull(request.nickname()))
                .set(User::getRealName, userDataSupport.trimToNull(request.realName()))
                .set(User::getEmail, email)
                .set(User::getPhone, phone)
                .set(User::getAvatarUrl, userDataSupport.trimToNull(request.avatarUrl()))
                .set(User::getUpdatedAt, now()));
        appendAudit(UserAuditActions.USER_PROFILE_UPDATED, userId, "管理员更新用户公共资料");
        appendLifecycle(userId, "PROFILE_UPDATED", requireUser(userId).getStatus());
        return detail(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Integer status) {
        if (userId.equals(StpUtil.getLoginIdAsLong()) && status == 0) {
            throw new BusinessException(ResultCode.CANNOT_OPERATE_SELF);
        }
        requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, status)
                .set(User::getUpdatedAt, now()));
        appendAudit(UserAuditActions.USER_STATUS_CHANGED, userId, "status=" + status);
        appendLifecycle(userId, "STATUS_CHANGED", status);
        if (status == 0) {
            StpUtil.kickout(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userId.equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ResultCode.CANNOT_OPERATE_SELF);
        }
        requireUser(userId);
        List<String> roleCodes = rbacService.roleCodesByIds(roleIds);
        profileService.validateRoleProfiles(userId, roleCodes);
        rbacService.replaceUserRoles(userId, roleIds);
        appendAudit(UserAuditActions.USER_ROLES_CHANGED, userId, "roleCount=" + roleIds.size());
        appendLifecycle(userId, "ROLES_CHANGED", requireUser(userId).getStatus());
        StpUtil.kickout(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        if (userId.equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ResultCode.CANNOT_OPERATE_SELF, "请通过个人账户接口修改自己的密码");
        }
        requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(newPassword))
                .set(User::getPasswordChangedAt, now())
                .set(User::getUpdatedAt, now()));
        appendAudit(UserAuditActions.USER_PASSWORD_RESET, userId, "管理员重置用户密码");
        appendLifecycle(userId, "PASSWORD_RESET", requireUser(userId).getStatus());
        StpUtil.kickout(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        if (userId.equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ResultCode.CANNOT_OPERATE_SELF);
        }
        requireUser(userId);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        userMapper.deleteById(userId);
        appendAudit(UserAuditActions.USER_DELETED, userId, "逻辑删除用户");
        eventOutboxService.appendLifecycle(userId, "DELETED", 0, List.of());
        StpUtil.kickout(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse upsertStudentProfile(Long userId, AdminStudentProfileRequest request) {
        requireUser(userId);
        profileService.upsertStudentProfile(
                userId, request.studentNo(), request.gradeLevel(), request.semester());
        appendAudit(UserAuditActions.USER_PROFILE_UPDATED, userId, "管理员更新学生资料");
        appendLifecycle(userId, "STUDENT_PROFILE_UPDATED", requireUser(userId).getStatus());
        return detail(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse upsertTeacherProfile(Long userId, AdminTeacherProfileRequest request) {
        requireUser(userId);
        profileService.upsertTeacherProfile(
                userId, request.teacherNo(), request.title(), request.introduction());
        appendAudit(UserAuditActions.USER_PROFILE_UPDATED, userId, "管理员更新教师资料");
        appendLifecycle(userId, "TEACHER_PROFILE_UPDATED", requireUser(userId).getStatus());
        return detail(userId);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    private AdminUserResponse toAdminUser(User user) {
        List<RoleSummaryResponse> roles = rbacService.roles(user.getId()).stream()
                .map(this::toRoleSimple)
                .toList();
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getStatus(),
                roles,
                user.getWechatOpenid() != null,
                profileService.studentProfile(user.getId()),
                profileService.teacherProfile(user.getId()),
                user.getLastLoginAt(),
                user.getLastLoginIp(),
                user.getPasswordChangedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private RoleSummaryResponse toRoleSimple(Role role) {
        return new RoleSummaryResponse(role.getId(), role.getCode(), role.getName());
    }

    private void appendAudit(String action, Long targetUserId, String detail) {
        eventOutboxService.appendAudit(
                MqTopology.AUDIT_OPERATION_ROUTING_KEY,
                StpUtil.getLoginIdAsLong(),
                action,
                "USER",
                targetUserId,
                detail);
    }

    private void appendLifecycle(Long userId, String action, Integer status) {
        eventOutboxService.appendLifecycle(userId, action, status, rbacService.roleCodes(userId));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
