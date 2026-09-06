package xyz.jiayuhan.contentservice.model.response;

import java.time.OffsetDateTime;

/** 管理端内容模块详情。 */
public record AdminContentModuleResponse(
        Long id,
        String code,
        String name,
        Integer gradeLevel,
        Integer semester,
        String learningTheme,
        String publisher,
        String editionDescription,
        String description,
        Integer sortOrder,
        Integer status,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Integer version
) {
}
