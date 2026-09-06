package xyz.jiayuhan.contentapi.model.response;

/** 错因字典稳定摘要。 */
public record ErrorTypeResponse(
        Long id,
        String code,
        String name,
        String category,
        String applicableQuestionType,
        String description,
        String explanationTemplate,
        String remediationHint,
        Integer status
) {
}
