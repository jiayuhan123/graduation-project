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
import xyz.jiayuhan.userservice.entity.Role;
import xyz.jiayuhan.userservice.entity.User;
import xyz.jiayuhan.userservice.mapper.UserMapper;
import xyz.jiayuhan.userservice.mapper.UserRoleMapper;
import xyz.jiayuhan.userservice.entity.UserRole;
import xyz.jiayuhan.userservice.model.dto.AdminCreateUserDTO;
import xyz.jiayuhan.userservice.model.dto.AdminUpdateUserDTO;
import xyz.jiayuhan.userservice.model.dto.RegisterDTO;
import xyz.jiayuhan.userservice.model.vo.AdminUserVO;
import xyz.jiayuhan.userservice.model.vo.PageVO;
import xyz.jiayuhan.userservice.model.vo.RegisterVO;
import xyz.jiayuhan.userservice.model.vo.RoleSimpleVO;
import xyz.jiayuhan.userservice.service.AdminUserService;
import xyz.jiayuhan.userservice.service.AuthService;
import xyz.jiayuhan.userservice.service.RbacService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户维护实现。
 *
 * <p>将资料、状态、角色和密码拆成独立操作，便于权限点细分；所有会改变账号
 * 安全状态的操作都会同步注销目标账号会话。</p>
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserDataSupport userDataSupport;
    private final RbacService rbacService;
    private final AuthService authService;
    private final UserRoleMapper userRoleMapper;

    @Override
    public PageVO<AdminUserVO> page(long page, long size, String keyword, Integer status) {
        // 将多个模糊匹配条件放进同一个 AND 分组，确保状态过滤对所有关键字字段生效。
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(status != null, User::getStatus, status)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword)
                        .or().like(User::getRealName, keyword)
                        .or().like(User::getEmail, keyword)
                        .or().like(User::getPhone, keyword))
                .orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        return PageVO.from(result, this::toAdminUser);
    }

    @Override
    public AdminUserVO detail(Long userId) {
        return toAdminUser(requireUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO create(AdminCreateUserDTO request) {
        // 复用注册流程，保证字段规范化、唯一性校验、密码哈希和默认角色规则只有一份实现。
        RegisterVO registered = authService.register(new RegisterDTO(
                request.username(),
                request.password(),
                request.nickname(),
                request.realName(),
                request.email(),
                request.phone()
        ));
        if (request.status() != null && request.status() == 0) {
            updateStatusInternal(registered.id(), 0);
        }
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            rbacService.replaceUserRoles(registered.id(), request.roleIds());
        }
        return detail(registered.id());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO update(Long userId, AdminUpdateUserDTO request) {
        requireUser(userId);
        String phone = userDataSupport.trimToNull(request.phone());
        String email = userDataSupport.normalizeEmail(request.email());
        userDataSupport.checkUnique(null, phone, email, userId);

        // 使用显式 set 而不是实体更新，使调用方能够用空白值主动清空可选资料字段。
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getNickname, userDataSupport.trimToNull(request.nickname()))
                .set(User::getRealName, userDataSupport.trimToNull(request.realName()))
                .set(User::getEmail, email)
                .set(User::getPhone, phone)
                .set(User::getAvatarUrl, userDataSupport.trimToNull(request.avatarUrl()))
                .set(User::getUpdatedAt, LocalDateTime.now()));
        return detail(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Integer status) {
        // 禁止管理员禁用自己，避免当前管理入口被误操作立即锁死。
        if (userId.equals(StpUtil.getLoginIdAsLong()) && status == 0) {
            throw new BusinessException(ResultCode.CANNOT_OPERATE_SELF);
        }
        requireUser(userId);
        updateStatusInternal(userId, status);
        if (status == 0) {
            // 禁用后立即撤销全部会话，不能等待原令牌自然过期。
            StpUtil.kickout(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 管理员不能修改自己的角色，防止误删自身权限或绕过双人复核流程。
        if (userId.equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ResultCode.CANNOT_OPERATE_SELF);
        }
        requireUser(userId);
        rbacService.replaceUserRoles(userId, roleIds);
        // 角色变更后注销旧会话，确保后续访问重新按最新授权状态登录。
        StpUtil.kickout(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        // 自助改密需要校验旧密码，管理员重置接口不得用来跳过这一身份复核。
        if (userId.equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ResultCode.CANNOT_OPERATE_SELF, "请通过个人账户接口修改自己的密码");
        }
        requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(newPassword))
                .set(User::getPasswordChangedAt, LocalDateTime.now())
                .set(User::getUpdatedAt, LocalDateTime.now()));
        // 密码重置属于凭证失效事件，撤销目标账号的全部现有会话。
        StpUtil.kickout(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        if (userId.equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ResultCode.CANNOT_OPERATE_SELF);
        }
        requireUser(userId);
        // t_user 使用逻辑删除，不会触发数据库 ON DELETE CASCADE，因此需显式清理角色关系。
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        userMapper.deleteById(userId);
        StpUtil.kickout(userId);
    }

    private void updateStatusInternal(Long userId, Integer status) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, status)
                .set(User::getUpdatedAt, LocalDateTime.now()));
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    private AdminUserVO toAdminUser(User user) {
        List<RoleSimpleVO> roles = rbacService.roles(user.getId()).stream()
                .map(this::toRoleSimple)
                .toList();
        return new AdminUserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getStatus(),
                roles,
                user.getLastLoginAt(),
                user.getLastLoginIp(),
                user.getPasswordChangedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private RoleSimpleVO toRoleSimple(Role role) {
        return new RoleSimpleVO(role.getId(), role.getCode(), role.getName());
    }
}
