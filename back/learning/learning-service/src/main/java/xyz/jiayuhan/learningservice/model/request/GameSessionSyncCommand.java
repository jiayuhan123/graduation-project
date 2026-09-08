package xyz.jiayuhan.learningservice.model.request;

import java.time.OffsetDateTime;

/** 将一名学生的游戏结算状态同步到对应学习任务。 */
public record GameSessionSyncCommand(
        Long sessionId,
        Long knowledgePointId,
        Integer gradeLevel,
        Integer difficulty,
        Integer questionCount,
        Integer timeLimitSeconds,
        String mode,
        String result,
        OffsetDateTime occurredAt
) {
}
