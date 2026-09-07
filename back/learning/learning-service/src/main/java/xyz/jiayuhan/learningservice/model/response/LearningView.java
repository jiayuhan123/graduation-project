package xyz.jiayuhan.learningservice.model.response;

import xyz.jiayuhan.learningapi.model.response.PracticeTaskSummaryResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 学习端查询接口使用的不可变响应模型集合。 */
public final class LearningView {

    private LearningView() {
    }

    public record PracticeQuestion(
            Long id,
            Long questionId,
            Integer sequenceNo,
            String questionType,
            String stem,
            String options,
            Integer difficulty,
            Integer estimatedSeconds,
            BigDecimal maxScore,
            Answer latestAnswer,
            boolean solutionVisible,
            String standardAnswer,
            String analysis
    ) {
    }

    public record PracticeDetail(
            PracticeTaskSummaryResponse task,
            List<Long> knowledgePointIds,
            List<PracticeQuestion> questions
    ) {
    }

    public record Answer(
            Long id,
            Long taskQuestionId,
            Integer attemptNo,
            String studentAnswer,
            String gradingStatus,
            Boolean correct,
            BigDecimal score,
            Integer responseMs,
            String answerSource,
            BigDecimal recognitionConfidence,
            OffsetDateTime submittedAt,
            OffsetDateTime gradedAt
    ) {
    }

    public record Error(
            Long id,
            Long answerRecordId,
            Long questionId,
            Long knowledgePointId,
            Long errorTypeId,
            String errorPosition,
            String diagnosis,
            BigDecimal confidence,
            String status,
            Long resolvedByAnswerId,
            OffsetDateTime resolvedAt
    ) {
    }

    public record Mastery(
            Long id,
            Long knowledgePointId,
            BigDecimal masteryScore,
            BigDecimal accuracyRate,
            Integer avgResponseMs,
            Integer totalAnswerCount,
            Integer correctAnswerCount,
            Integer consecutiveCorrectCount,
            OffsetDateTime lastPracticedAt,
            OffsetDateTime nextReviewAt
    ) {
    }

    public record MasteryChange(
            Long id,
            Long answerRecordId,
            BigDecimal beforeScore,
            BigDecimal afterScore,
            BigDecimal deltaScore,
            String reason,
            OffsetDateTime occurredAt
    ) {
    }

    public record RecommendationItem(
            Long id,
            Long knowledgePointId,
            String sourceType,
            Long sourceId,
            String reasonText,
            Integer priority,
            Integer recommendedQuestionCount,
            String status,
            Long generatedTaskId,
            OffsetDateTime generatedAt,
            OffsetDateTime expiresAt,
            OffsetDateTime acceptedAt,
            OffsetDateTime completedAt
    ) {
    }

    public record PhotoTask(
            Long id,
            UUID originalFileId,
            Long practiceTaskId,
            String status,
            Integer pageCount,
            BigDecimal overallConfidence,
            Integer recognizedItemCount,
            String reviewStatus,
            OffsetDateTime submittedAt,
            OffsetDateTime processedAt,
            OffsetDateTime reviewedAt,
            String failureReason
    ) {
    }

    public record PhotoItem(
            Long id,
            Integer itemNo,
            UUID cropFileId,
            String boundingBox,
            String recognizedStem,
            String recognizedAnswer,
            Long matchedQuestionId,
            BigDecimal recognitionConfidence,
            Long taskQuestionId,
            Long answerRecordId,
            String status
    ) {
    }

    public record PhotoDetail(PhotoTask task, List<PhotoItem> items) {
    }

    public record AnswerFeedback(
            Long taskId,
            Long taskQuestionId,
            Answer answer,
            boolean solutionVisible,
            String standardAnswer,
            String analysis
    ) {
    }

    public record Overview(
            Long studentId,
            long taskCount,
            long completedTaskCount,
            long answerCount,
            long correctAnswerCount,
            BigDecimal accuracyRate,
            long openErrorCount,
            long weakKnowledgePointCount,
            long pendingRecommendationCount
    ) {
    }

    public record StudentReport(
            Long studentId,
            String username,
            String displayName,
            String avatarUrl,
            Overview overview,
            List<PracticeTaskSummaryResponse> recentTasks,
            List<Mastery> weakKnowledgePoints,
            List<Error> recentErrors,
            List<RecommendationItem> recommendations
    ) {
    }
}
