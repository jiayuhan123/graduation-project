package xyz.jiayuhan.teachingservice.model.response;

/** 管理员运营看板的教学域统计。 */
public record TeachingOperationsStatisticsResponse(
        long totalClasses,
        long activeClasses,
        long teacherMemberships,
        long activeStudentMemberships,
        long totalAssignments,
        long publishedAssignments,
        long totalSubmissions,
        long completedSubmissions
) {
}
