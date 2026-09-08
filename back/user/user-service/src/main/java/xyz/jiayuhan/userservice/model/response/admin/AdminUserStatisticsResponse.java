package xyz.jiayuhan.userservice.model.response.admin;

/** 管理员运营看板的用户域统计。 */
public record AdminUserStatisticsResponse(
        long totalUsers,
        long activeUsers,
        long disabledUsers,
        long studentUsers,
        long teacherUsers,
        long adminUsers
) {
}
