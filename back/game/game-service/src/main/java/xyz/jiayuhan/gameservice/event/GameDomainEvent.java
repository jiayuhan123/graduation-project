package xyz.jiayuhan.gameservice.event;

import xyz.jiayuhan.common.mq.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** 游戏服务向学习与通知场景发布的通用不可变事件。 */
public record GameDomainEvent(
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
