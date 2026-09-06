package xyz.jiayuhan.contentapi.model.response;

/** 内容模块稳定摘要。 */
public record ContentModuleResponse(
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
        Integer status
) {
}
