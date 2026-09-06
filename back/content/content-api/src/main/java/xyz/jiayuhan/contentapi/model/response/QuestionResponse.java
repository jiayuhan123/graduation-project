package xyz.jiayuhan.contentapi.model.response;

/** 供练习、作业和游戏服务使用的不可变题目快照来源。 */
public record QuestionResponse(
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
        String status
) {
}
