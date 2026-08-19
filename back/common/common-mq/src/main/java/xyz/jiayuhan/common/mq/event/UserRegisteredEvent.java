package xyz.jiayuhan.common.mq.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户注册成功事件。
 *
 * @param eventId 全局事件 ID，消费者据此实现幂等
 * @param eventType 稳定事件类型
 * @param occurredAt 事件发生时间
 * @param source 事件来源服务
 * @param userId 新用户 ID
 * @param username 用户名
 * @param email 邮箱，可为空
 */
public record UserRegisteredEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String source,
        Long userId,
        String username,
        String email
) implements DomainEvent {

    public static final String TYPE = "user.registered.v1";

    /** 创建一个由用户服务产生的新注册事件。 */
    public static UserRegisteredEvent create(Long userId, String username, String email) {
        return new UserRegisteredEvent(
                UUID.randomUUID(), TYPE, Instant.now(), "user-service", userId, username, email);
    }
}
