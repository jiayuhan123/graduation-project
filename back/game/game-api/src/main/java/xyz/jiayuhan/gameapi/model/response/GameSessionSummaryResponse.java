package xyz.jiayuhan.gameapi.model.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 学习服务读取的游戏对局最小契约。 */
public record GameSessionSummaryResponse(
        Long id,
        Long levelId,
        String mode,
        Long creatorStudentId,
        String matchMode,
        String inviteCode,
        String status,
        Integer memberCount,
        Integer gradeLevelSnapshot,
        Long knowledgePointIdSnapshot,
        Integer difficultySnapshot,
        Integer questionCountSnapshot,
        Integer timeLimitSecondsSnapshot,
        BigDecimal passRateSnapshot,
        BigDecimal teamProgress,
        Long winnerStudentId,
        String resultType,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime settledAt,
        String resultDetail
) {
}
