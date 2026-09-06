package xyz.jiayuhan.contentservice.model.response;

/** 学生端可见的题面；标准答案、解析和生成规则只保留在服务端。 */
public record PublishedQuestionResponse(
        Long id,
        String code,
        String questionMode,
        String questionType,
        String stem,
        String options,
        Integer difficulty,
        Integer estimatedSeconds
) {
}
