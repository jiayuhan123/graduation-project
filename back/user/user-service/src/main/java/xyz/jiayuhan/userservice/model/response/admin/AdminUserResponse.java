package xyz.jiayuhan.userservice.model.response.admin;

import xyz.jiayuhan.userservice.model.response.account.StudentProfileResponse;
import xyz.jiayuhan.userservice.model.response.account.TeacherProfileResponse;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 管理端用户详情；刻意排除密码哈希和逻辑删除标记。
 *
 * @param id 用户 ID
 * @param username 登录用户名
 * @param nickname 昵称
 * @param realName 真实姓名
 * @param email 邮箱
 * @param phone 手机号
 * @param avatarUrl 头像地址
 * @param status 账号状态：1 正常、0 禁用
 * @param roles 当前已启用角色
 * @param wechatBound 是否已绑定微信
 * @param studentProfile 学生资料
 * @param teacherProfile 教师资料
 * @param lastLoginAt 最近登录时间
 * @param lastLoginIp 最近登录 IP
 * @param passwordChangedAt 最近修改密码时间
 * @param createdAt 创建时间
 * @param updatedAt 最近更新时间
 */
public record AdminUserResponse(
        Long id,
        String username,
        String nickname,
        String realName,
        String email,
        String phone,
        String avatarUrl,
        Integer status,
        List<RoleSummaryResponse> roles,
        boolean wechatBound,
        StudentProfileResponse studentProfile,
        TeacherProfileResponse teacherProfile,
        OffsetDateTime lastLoginAt,
        String lastLoginIp,
        OffsetDateTime passwordChangedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
