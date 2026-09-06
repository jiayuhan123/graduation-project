package xyz.jiayuhan.contentservice.model.response;

import java.time.OffsetDateTime;
import java.util.List;

/** 管理端题目完整详情。 */
public record AdminQuestionDetailResponse(
        Long id,
        String code,
        String questionMode,
        String questionType,
        String stem,
        String options,
        String standardAnswer,
        String analysis,
        String generationRule,
        Integer difficulty,
        Integer estimatedSeconds,
        String status,
        Long createdBy,
        Long reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Integer version,
        List<QuestionKnowledgePointBindingResponse> knowledgePoints
) {
}
