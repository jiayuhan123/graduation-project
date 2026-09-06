package xyz.jiayuhan.contentapi.model.response;

/** 知识点稳定摘要。 */
public record KnowledgePointResponse(
        Long id,
        Long moduleId,
        Long parentId,
        String code,
        String name,
        String description,
        Integer difficultyLevel,
        Integer sortOrder,
        Integer status
) {
}
