package xyz.jiayuhan.userservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;
import xyz.jiayuhan.common.mq.event.DomainEvent;
import xyz.jiayuhan.userservice.entity.OutboxEvent;
import xyz.jiayuhan.userservice.mapper.OutboxEventMapper;

import java.time.LocalDateTime;

/** 把用户领域事件写入与业务数据同库、同事务的发件箱。 */
@Service
@RequiredArgsConstructor
public class UserEventOutboxService {

    private static final String PENDING = "PENDING";

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    /**
     * 将事件加入待发布队列。
     *
     * <p>MANDATORY 强制调用方已有数据库事务，防止业务提交而事件入箱失败。</p>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String routingKey, DomainEvent event) {
        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(event.eventId());
        outbox.setEventType(event.eventType());
        outbox.setRoutingKey(routingKey);
        outbox.setPayload(objectMapper.writeValueAsString(event));
        outbox.setStatus(PENDING);
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(LocalDateTime.now());
        outbox.setCreatedAt(LocalDateTime.now());
        outboxEventMapper.insert(outbox);
    }

    /** 创建并入箱一条不含敏感信息的用户服务操作审计事件。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendAudit(
            String routingKey,
            Long operatorId,
            String action,
            String targetType,
            Object targetId,
            String detail) {
        append(routingKey, AuditLogEvent.create(
                "user-service",
                operatorId,
                action,
                targetType,
                String.valueOf(targetId),
                detail));
    }
}
