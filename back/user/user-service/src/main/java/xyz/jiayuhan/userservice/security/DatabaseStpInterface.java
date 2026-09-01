package xyz.jiayuhan.userservice.security;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.jiayuhan.userservice.service.RbacService;

import java.util.List;

/**
 * Sa-Token 与数据库 RBAC 模型之间的适配器。
 *
 * <p>Sa-Token 在执行注解鉴权时回调本组件，从 PostgreSQL 查询当前有效角色和权限，
 * 因而数据库授权关系是唯一事实来源。</p>
 */
@Component
@RequiredArgsConstructor
public class DatabaseStpInterface implements StpInterface {

    private final RbacService rbacService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // Sa-Token 的登录 ID 类型为 Object，系统登录时统一写入 Long 用户 ID。
        return rbacService.permissionCodes(Long.valueOf(loginId.toString()));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return rbacService.roleCodes(Long.valueOf(loginId.toString()));
    }
}
