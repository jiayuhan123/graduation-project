package xyz.jiayuhan.contentservice.model.response;

import java.time.OffsetDateTime;

/** 管理端错因类型详情。 */
public record AdminErrorTypeResponse(
        Long id,
        String code,
        String name,
        String category,
        String applicableQuestionType,
        String description,
        String explanationTemplate,
        String remediationHint,
        Integer status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Integer version
) {
}
