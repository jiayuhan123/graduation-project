package xyz.jiayuhan.userservice.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.userservice.entity.Role;
import xyz.jiayuhan.userservice.entity.User;
import xyz.jiayuhan.userservice.mapper.RoleMapper;
import xyz.jiayuhan.userservice.mapper.UserMapper;
import xyz.jiayuhan.userservice.service.RbacService;
import xyz.jiayuhan.userservice.service.impl.UserDataSupport;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 按环境变量幂等准备初始管理员账号。
 *
 * <p>仅在显式启用配置时运行：账号不存在则走标准注册流程创建，账号已存在则只确保
 * 其处于启用状态并拥有 admin 角色，不覆盖已有密码。密码必须由部署环境注入，仓库中
 * 不保存任何默认管理员凭证。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final RbacService rbacService;
    private final UserDataSupport userDataSupport;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:admin}")
    private String username;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        // 启用引导功能却未提供合规密码时直接中止启动，避免生成弱口令管理员。
        if (password == null || password.length() < 6 || password.length() > 72) {
            throw new IllegalStateException("启用初始管理员时，BOOTSTRAP_ADMIN_PASSWORD 长度必须为 6-72");
        }

        String normalizedUsername = userDataSupport.normalizeUsername(username);
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, normalizedUsername));
        if (user == null) {
            String normalizedEmail = userDataSupport.normalizeEmail(email);
            userDataSupport.checkUnique(normalizedUsername, null, normalizedEmail, null);
            user = new User();
            user.setUsername(normalizedUsername);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setNickname("系统管理员");
            user.setEmail(normalizedEmail);
            user.setStatus(1);
            user.setPasswordChangedAt(OffsetDateTime.now(ZoneOffset.UTC));
            user.setDeleted(false);
            user.setVersion(0);
            userMapper.insert(user);
        } else if (!Integer.valueOf(1).equals(user.getStatus())) {
            // 已存在账号不重置密码，仅恢复状态，避免每次重启破坏人工修改后的凭证。
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, user.getId())
                    .set(User::getStatus, 1)
                    .set(User::getUpdatedAt, OffsetDateTime.now(ZoneOffset.UTC)));
        }

        Role adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, "admin"));
        if (adminRole == null) {
            throw new IllegalStateException("数据库迁移未创建 admin 角色");
        }
        // 引导账号最终只保留管理员角色；该角色已在迁移脚本中拥有完整管理权限。
        rbacService.replaceUserRoles(user.getId(), List.of(adminRole.getId()));
        log.info("初始管理员已就绪：userId={}, username={}", user.getId(), user.getUsername());
    }
}
