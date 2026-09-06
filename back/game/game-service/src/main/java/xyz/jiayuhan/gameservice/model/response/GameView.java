package xyz.jiayuhan.gameservice.model.response;

import xyz.jiayuhan.gameapi.model.response.GameSessionSummaryResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** 游戏查询接口使用的不可变响应模型集合。 */
public final class GameView {

    private GameView() {
    }

    public record Level(
            Long id,
            String code,
            String name,
            String description,
            Integer gradeLevel,
            Long knowledgePointId,
            String mode,
            Integer questionCount,
            Integer timeLimitSeconds,
            BigDecimal passRate,
            Integer minPlayers,
            Integer maxPlayers,
            Integer basePoints,
            Integer baseExperience,
            Long previousLevelId,
            Integer sortOrder,
            String status,
            String ruleConfig
    ) {
    }

    public record SessionMember(
            Long studentId,
            Integer seatNo,
            String status,
            Long practiceTaskId,
            Integer answeredCount,
            Integer correctCount,
            BigDecimal score,
            BigDecimal contributionValue,
            Integer maxCombo,
            OffsetDateTime joinedAt,
            OffsetDateTime finishedAt
    ) {
    }

    public record SessionQuestion(
            Long id,
            Integer sequenceNo,
            Long questionId,
            Long answerRecordId,
            Boolean correct,
            BigDecimal progressDelta,
            Integer energyDelta,
            OffsetDateTime answeredAt
    ) {
    }

    public record SessionDetail(
            GameSessionSummaryResponse session,
            List<SessionMember> members,
            List<SessionQuestion> myQuestions
    ) {
    }

    public record PlayerProgress(
            Long studentId,
            Long totalPoints,
            Long experience,
            Integer playerLevel,
            Integer totalSessions,
            Integer successSessions,
            Integer bestCombo
    ) {
    }

    public record LevelProgress(
            Long levelId,
            String status,
            Integer attemptCount,
            BigDecimal bestScore,
            Integer bestDurationSeconds,
            OffsetDateTime firstUnlockedAt,
            OffsetDateTime firstPassedAt,
            OffsetDateTime lastPlayedAt
    ) {
    }

    public record Reward(
            Long id,
            Long sessionId,
            String rewardType,
            Long amount,
            Long balanceAfter,
            String reason,
            OffsetDateTime createdAt
    ) {
    }
}
