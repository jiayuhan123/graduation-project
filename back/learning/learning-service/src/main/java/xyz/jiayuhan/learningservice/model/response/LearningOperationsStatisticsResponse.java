package xyz.jiayuhan.learningservice.model.response;

import java.math.BigDecimal;

/** 管理员运营看板的学习和 OCR 域统计。 */
public record LearningOperationsStatisticsResponse(
        long totalPracticeTasks,
        long completedPracticeTasks,
        long totalAnswers,
        long correctAnswers,
        BigDecimal answerAccuracyRate,
        long totalOcrTasks,
        long completedOcrTasks,
        long failedOcrTasks,
        long pendingReviewTasks,
        long totalRecognizedItems,
        long manuallyReviewedItems,
        long approvedItems,
        long correctedItems,
        long rejectedItems,
        BigDecimal ocrFailureRate,
        BigDecimal manualReviewRate,
        BigDecimal reviewAccuracyRate
) {
}
