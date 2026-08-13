package xyz.jiayuhan.userservice.model.vo;

/**
 * 注册成功返回数据
 *
 * @param id       新用户主键 ID
 * @param username 用户名
 */
public record RegisterVO(
        Long id,
        String username
) {
}
