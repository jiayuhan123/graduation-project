package xyz.jiayuhan.userservice.model.vo;

import java.time.LocalDateTime;

/**
 * 登录成功返回数据
 *
 * @param token     登录 Token
 * @param tokenName Token 请求头名称（satoken）
 * @param userId    用户主键 ID
 * @param username  用户名
 * @param role      角色（默认 resident，社区模块上线后取自社区成员关系）
 * @param expireAt  Token 过期时间
 */
public record LoginVO(
        String token,
        String tokenName,
        Long userId,
        String username,
        String role,
        LocalDateTime expireAt
) {
}
