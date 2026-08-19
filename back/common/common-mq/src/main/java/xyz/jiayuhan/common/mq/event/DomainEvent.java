package xyz.jiayuhan.common.mq.event;

import java.time.Instant;
import java.util.UUID;

/** 所有跨服务领域事件都必须提供的不可变元数据。 */
public interface DomainEvent {

    UUID eventId();

    String eventType();

    Instant occurredAt();

    String source();
}
