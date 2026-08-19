package xyz.jiayuhan.common.mq.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 跨服务操作审计事件，禁止在 detail 中写入密码、令牌等敏感信息。
 *
 * @param eventId 全局事件 ID
 * @param eventType 稳定事件类型
 * @param occurredAt 操作发生时间
 * @param source 来源服务
 * @param operatorId 操作用户 ID
 * @param action 稳定操作编码
 * @param targetType 操作对象类型
 * @param targetId 操作对象 ID
 * @param detail 不含敏感数据的补充说明
 */
public record AuditLogEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String source,
        Long operatorId,
        String action,
        String targetType,
        String targetId,
        String detail
) implements DomainEvent {

    public static final String TYPE = "audit.operation.v1";

    /** 创建一条成功操作审计事件。 */
    public static AuditLogEvent create(
            String source,
            Long operatorId,
            String action,
            String targetType,
            String targetId,
            String detail) {
        return new AuditLogEvent(
                UUID.randomUUID(), TYPE, Instant.now(), source,
                operatorId, action, targetType, targetId, detail);
    }
}
