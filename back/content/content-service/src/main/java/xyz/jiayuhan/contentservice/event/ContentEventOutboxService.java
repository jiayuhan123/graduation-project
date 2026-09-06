package xyz.jiayuhan.contentservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;
import xyz.jiayuhan.contentservice.entity.ContentOutboxEvent;
import xyz.jiayuhan.contentservice.mapper.ContentOutboxEventMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 将内容变更审计事件与业务数据写入同一个数据库事务。 */
@Service
@RequiredArgsConstructor
public class ContentEventOutboxService {

    private static final String PENDING = "PENDING";

    private final ContentOutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void appendAudit(
            Long operatorId,
            String action,
            String targetType,
            Object targetId,
            String detail) {
        AuditLogEvent event = AuditLogEvent.create(
                "content-service", operatorId, action, targetType, String.valueOf(targetId), detail);
        ContentOutboxEvent outbox = new ContentOutboxEvent();
        outbox.setId(event.eventId());
        outbox.setEventType(event.eventType());
        outbox.setRoutingKey(MqTopology.AUDIT_OPERATION_ROUTING_KEY);
        outbox.setPayload(objectMapper.writeValueAsString(event));
        outbox.setStatus(PENDING);
        outbox.setRetryCount(0);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        outbox.setNextRetryAt(now);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);
        outboxEventMapper.insert(outbox);
    }
}
