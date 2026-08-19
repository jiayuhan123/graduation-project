package xyz.jiayuhan.userservice.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 可安全对外返回的当前用户资料，不包含密码哈希和逻辑删除标记。
 *
 * @param id 用户 ID
 * @param username 登录用户名
 * @param nickname 昵称
 * @param realName 真实姓名
 * @param email 邮箱
 * @param phone 手机号
 * @param avatarUrl 头像地址
 * @param status 账号状态：1 正常、0 禁用
 * @param roles 当前已启用角色编码
 * @param permissions 当前权限编码
 * @param lastLoginAt 最近登录时间
 * @param createdAt 创建时间
 * @param updatedAt 最近更新时间
 */
public record UserProfileVO(
        Long id,
        String username,
        String nickname,
        String realName,
        String email,
        String phone,
        String avatarUrl,
        Integer status,
        List<String> roles,
        List<String> permissions,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
