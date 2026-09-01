package xyz.jiayuhan.common.mq.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 用户状态、资料、凭证或角色发生变化的跨服务事件。 */
public record UserLifecycleEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String source,
        Long userId,
        String action,
        Integer status,
        List<String> roles
) implements DomainEvent {

    public static final String TYPE = "user.lifecycle.changed.v1";

    public static UserLifecycleEvent create(
            Long userId,
            String action,
            Integer status,
            List<String> roles) {
        return new UserLifecycleEvent(
                UUID.randomUUID(),
                TYPE,
                Instant.now(),
                "user-service",
                userId,
                action,
                status,
                List.copyOf(roles));
    }
}
