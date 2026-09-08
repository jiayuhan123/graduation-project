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
            Integer difficulty,
            String questionSource,
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
            String memberRole,
            String status,
            String result,
            Long practiceTaskId,
            Integer answeredCount,
            Integer correctCount,
            BigDecimal score,
            BigDecimal contributionValue,
            Integer currentCombo,
            Integer maxCombo,
            Long totalResponseMs,
            Integer ratingBefore,
            Integer ratingAfter,
            Integer pointsReward,
            Integer experienceReward,
            OffsetDateTime joinedAt,
            OffsetDateTime lastAnsweredAt,
            OffsetDateTime finishedAt
    ) {
    }

    public record SessionQuestion(
            Long id,
            Integer sequenceNo,
            Long questionId,
            String gradingStatus,
            Long answerRecordId,
            Boolean correct,
            BigDecimal score,
            Integer responseMs,
            Integer comboAfter,
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
            Integer levelPassedCount,
            Integer pkSessions,
            Integer pkWins,
            Integer pkLosses,
            Integer pkDraws,
            Integer pkRating,
            Integer currentWinStreak,
            Integer bestWinStreak,
            Integer bestCombo,
            OffsetDateTime lastPlayedAt
    ) {
    }

    public record LevelProgress(
            Long levelId,
            String status,
            Integer attemptCount,
            BigDecimal bestScore,
            Integer bestCorrectCount,
            Integer bestStarCount,
            Integer bestDurationSeconds,
            Long lastSessionId,
            OffsetDateTime firstUnlockedAt,
            OffsetDateTime firstPassedAt,
            OffsetDateTime lastPlayedAt
    ) {
    }

    public record Reward(
            Long id,
            Long sessionId,
            String businessType,
            String rewardType,
            Long amount,
            Long balanceAfter,
            String reason,
            OffsetDateTime createdAt
    ) {
    }
}
