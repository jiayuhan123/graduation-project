package xyz.jiayuhan.common.mq.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** 游戏逐题结果和对局结算向学习域发布的稳定跨服务事件。 */
public record GameLearningEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String source,
        String aggregateType,
        Long aggregateId,
        Long studentId,
        Map<String, Object> data
) implements DomainEvent {
}
