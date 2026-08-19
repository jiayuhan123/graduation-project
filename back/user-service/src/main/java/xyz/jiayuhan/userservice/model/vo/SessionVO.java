package xyz.jiayuhan.userservice.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前会话信息。
 *
 * @param userId 当前用户 ID
 * @param token 当前令牌值
 * @param tokenName 令牌请求头名称
 * @param roles 当前已启用角色编码
 * @param permissions 当前权限编码
 * @param timeout 令牌剩余有效秒数；负数表示框架定义的长期有效状态
 * @param expireAt 预计过期时间；长期有效时为 {@code null}
 */
public record SessionVO(
        Long userId,
        String token,
        String tokenName,
        List<String> roles,
        List<String> permissions,
        long timeout,
        LocalDateTime expireAt
) {
}
