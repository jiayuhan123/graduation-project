package xyz.jiayuhan.teachingservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;
import xyz.jiayuhan.teachingservice.entity.TeachingOutboxEvent;
import xyz.jiayuhan.teachingservice.mapper.TeachingOutboxEventMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 将教学变更审计事件与业务数据写入同一个数据库事务。 */
@Service
@RequiredArgsConstructor
public class TeachingEventOutboxService {

    private final TeachingOutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void appendAudit(
            Long operatorId,
            String action,
            String targetType,
            Object targetId,
            String detail) {
        AuditLogEvent event = AuditLogEvent.create(
                "teaching-service", operatorId, action, targetType, String.valueOf(targetId), detail);
        TeachingOutboxEvent outbox = new TeachingOutboxEvent();
        outbox.setId(event.eventId());
        outbox.setEventType(event.eventType());
        outbox.setRoutingKey(MqTopology.AUDIT_OPERATION_ROUTING_KEY);
        outbox.setPayload(objectMapper.writeValueAsString(event));
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        outbox.setNextRetryAt(now);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);
        outboxEventMapper.insert(outbox);
    }
}
