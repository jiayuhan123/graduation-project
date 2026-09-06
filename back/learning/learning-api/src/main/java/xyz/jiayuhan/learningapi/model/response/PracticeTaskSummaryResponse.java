package xyz.jiayuhan.learningapi.model.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 教学和游戏服务读取的练习任务最小契约。 */
public record PracticeTaskSummaryResponse(
        Long id,
        Long studentId,
        String sourceType,
        String sourceId,
        String title,
        String mode,
        String status,
        Integer questionCount,
        Integer timeLimitSeconds,
        BigDecimal targetAccuracy,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
}
