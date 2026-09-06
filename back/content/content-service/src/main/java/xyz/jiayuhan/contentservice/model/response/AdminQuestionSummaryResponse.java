package xyz.jiayuhan.contentservice.model.response;

import java.time.OffsetDateTime;

/** 管理端题目列表摘要。 */
public record AdminQuestionSummaryResponse(
        Long id,
        String code,
        String questionMode,
        String questionType,
        String stem,
        Integer difficulty,
        Integer estimatedSeconds,
        String status,
        Long createdBy,
        Long reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime updatedAt,
        Integer version
) {
}
