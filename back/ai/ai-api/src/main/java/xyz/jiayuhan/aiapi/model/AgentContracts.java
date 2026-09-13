package xyz.jiayuhan.aiapi.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Java、Python 与前端共同使用的不可变 Agent 协议。 */
public final class AgentContracts {

    private AgentContracts() {
    }

    public record ChatRequest(
            UUID conversationId,
            @NotBlank
            @Pattern(regexp = "teacher|admin", message = "当前角色暂不支持教学 Agent") String role,
            @NotBlank(message = "消息不能为空")
            @Size(max = 4000, message = "单条消息不能超过4000个字符") String message
    ) {
    }

    public record Actor(
            @NotNull Long userId,
            String displayName,
            @NotBlank String activeRole,
            @NotNull List<String> roles,
            @NotNull List<String> allowedTools
    ) {
    }

    public record RunRequest(
            @NotNull UUID runId,
            @NotNull UUID conversationId,
            @NotBlank String message,
            @NotNull Actor actor,
            @NotBlank String toolToken
    ) {
    }

    public record ToolInvocationRequest(
            @NotNull UUID runId,
            @NotBlank
            @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$", message = "工具名称格式不正确")
            String toolName,
            @NotNull Map<String, Object> arguments
    ) {
    }

    public record ToolResult(
            String toolName,
            Object data
    ) {
    }

    public record ClassMetrics(
            int studentCount,
            long taskCount,
            long completedTaskCount,
            BigDecimal completionRate,
            long answerCount,
            long correctAnswerCount,
            BigDecimal accuracyRate,
            long openErrorCount
    ) {
    }

    public record WeakKnowledgePoint(
            Long knowledgePointId,
            String knowledgePointName,
            BigDecimal averageMastery,
            BigDecimal averageAccuracy,
            int affectedStudentCount,
            int assessedStudentCount
    ) {
    }

    public record FrequentWrongQuestion(
            Long questionId,
            String questionCode,
            String stem,
            long errorCount,
            int affectedStudentCount
    ) {
    }

    public record FrequentErrorType(
            Long errorTypeId,
            String errorTypeName,
            String category,
            long errorCount,
            int affectedStudentCount
    ) {
    }

    public record DailyTrend(
            LocalDate date,
            long answerCount,
            long correctAnswerCount,
            BigDecimal accuracyRate
    ) {
    }

    public record StudentAlert(
            Long studentId,
            String displayName,
            String code,
            String message,
            String severity
    ) {
    }

    public record ClassDashboard(
            Long classId,
            String className,
            String schoolName,
            Integer gradeLevel,
            Integer semester,
            int rangeDays,
            OffsetDateTime rangeStart,
            OffsetDateTime rangeEnd,
            ClassMetrics metrics,
            List<WeakKnowledgePoint> weakKnowledgePoints,
            List<FrequentWrongQuestion> frequentWrongQuestions,
            List<FrequentErrorType> frequentErrorTypes,
            List<DailyTrend> dailyTrends,
            List<StudentAlert> alerts
    ) {
    }

    public record QuestionCandidate(
            Long id,
            String code,
            String questionType,
            String stem,
            Integer difficulty,
            Integer estimatedSeconds
    ) {
    }

    public record QuestionPage(
            long page,
            long size,
            long total,
            long pages,
            List<QuestionCandidate> records
    ) {
    }

    public record AssignmentQuestion(
            @NotNull Long questionId,
            @NotBlank
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{0,31}$", message = "题目层级格式不正确")
            String layerCode,
            @NotNull @Min(1) @Max(100) Integer sequenceNo,
            @NotNull @DecimalMin("0.01") @DecimalMax("999999.99") BigDecimal score
    ) {
    }

    public record AssignmentDraftCommand(
            @NotNull Long classId,
            @NotBlank @Size(max = 128) String title,
            @Size(max = 1000) String description,
            @NotBlank @Pattern(regexp = "UNIFIED|LAYERED") String assignmentType,
            @NotNull OffsetDateTime startAt,
            @NotNull OffsetDateTime dueAt,
            String settings,
            @NotEmpty @Size(max = 100) List<@Valid AssignmentQuestion> questions
    ) {
    }

    public record ActionDraft(
            UUID draftId,
            String actionType,
            String title,
            String summary,
            OffsetDateTime expiresAt,
            String confirmationToken
    ) {
    }

    public record ConfirmActionRequest(
            @NotBlank(message = "确认令牌不能为空") String confirmationToken
    ) {
    }

    public record AssignmentCreated(
            Long assignmentId,
            Long classId,
            String title,
            String status,
            Integer version
    ) {
    }

    public record FeedbackRequest(
            @NotNull UUID runId,
            @NotNull @Min(-1) @Max(1) Integer rating,
            @Size(max = 1000) String comment
    ) {
    }

    public record KnowledgeUpsertRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 1000) String sourceUri,
            @NotBlank
            @Pattern(regexp = "TEXT|MARKDOWN|POLICY|LESSON|TEXTBOOK") String sourceType,
            @NotBlank @Size(max = 100000) String content,
            @NotNull Map<String, Object> metadata
    ) {
    }

    public record KnowledgeDocument(
            UUID id,
            String title,
            String sourceUri,
            String sourceType,
            String checksum,
            Integer chunkCount,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }
}
