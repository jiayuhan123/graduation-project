package xyz.jiayuhan.learningservice.model.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 将游戏服务已经可信判定的一次作答同步到统一学习档案。 */
public record GameAnswerSyncCommand(
        Long sessionId,
        Long sessionQuestionId,
        Integer sequenceNo,
        Long questionId,
        Long knowledgePointId,
        Integer gradeLevel,
        Integer difficulty,
        Integer questionCount,
        Integer timeLimitSeconds,
        String mode,
        String studentAnswer,
        Boolean correct,
        BigDecimal score,
        Integer responseMs,
        OffsetDateTime occurredAt
) {
}
