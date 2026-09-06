package xyz.jiayuhan.gameapi.model.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 学习服务读取的游戏对局最小契约。 */
public record GameSessionSummaryResponse(
        Long id,
        Long levelId,
        String mode,
        Long creatorStudentId,
        String status,
        Integer memberCount,
        Integer questionCountSnapshot,
        Integer timeLimitSecondsSnapshot,
        BigDecimal passRateSnapshot,
        BigDecimal teamProgress,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String resultDetail
) {
}
