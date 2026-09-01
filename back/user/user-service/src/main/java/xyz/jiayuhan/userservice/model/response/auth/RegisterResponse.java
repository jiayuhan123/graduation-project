package xyz.jiayuhan.userservice.model.response.auth;

/**
 * 注册成功响应。
 *
 * @param id 新用户主键 ID
 * @param username 用户名
 */
public record RegisterResponse(
        Long id,
        String username
) {
}
