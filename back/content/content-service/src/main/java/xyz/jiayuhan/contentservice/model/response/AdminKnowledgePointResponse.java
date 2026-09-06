package xyz.jiayuhan.contentservice.model.response;

import java.time.OffsetDateTime;

/** 管理端知识点详情。 */
public record AdminKnowledgePointResponse(
        Long id,
        Long moduleId,
        Long parentId,
        String code,
        String name,
        String description,
        Integer difficultyLevel,
        Integer sortOrder,
        Integer status,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Integer version
) {
}
