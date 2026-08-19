package xyz.jiayuhan.userservice.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录成功响应。
 *
 * @param token Sa-Token 令牌值
 * @param tokenName 客户端传递令牌时使用的请求头名称
 * @param userId 用户 ID
 * @param username 用户名
 * @param role 主角色；为兼容既有客户端保留，新的调用方应使用 {@code roles}
 * @param roles 当前已启用角色编码
 * @param permissions 当前权限编码
 * @param timeout 令牌剩余有效秒数；负数表示框架定义的长期有效状态
 * @param expireAt 预计过期时间；长期有效时为 {@code null}
 */
public record LoginVO(
        String token,
        String tokenName,
        Long userId,
        String username,
        String role,
        List<String> roles,
        List<String> permissions,
        long timeout,
        LocalDateTime expireAt
) {
}
