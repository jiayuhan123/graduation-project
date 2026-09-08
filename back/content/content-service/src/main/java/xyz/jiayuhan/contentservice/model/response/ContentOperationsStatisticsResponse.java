package xyz.jiayuhan.contentservice.model.response;

/** 管理员运营看板的内容域统计。 */
public record ContentOperationsStatisticsResponse(
        long totalModules,
        long activeModules,
        long totalKnowledgePoints,
        long activeKnowledgePoints,
        long totalQuestions,
        long enabledQuestions,
        long draftQuestions,
        long disabledQuestions,
        long pendingFeedback
) {
}
