package xyz.jiayuhan.contentservice.model.response;

import java.time.OffsetDateTime;

/** 题目反馈及其处理轨迹。 */
public record QuestionFeedbackResponse(
        Long id,
        Long questionId,
        String questionCode,
        String questionStem,
        String questionStatus,
        Long reporterId,
        String category,
        String description,
        String status,
        Long handlerId,
        String handleComment,
        OffsetDateTime handledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Integer version
) {
}
