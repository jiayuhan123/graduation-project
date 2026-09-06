package xyz.jiayuhan.teachingservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;
import xyz.jiayuhan.common.mq.publisher.DomainEventPublisher;
import xyz.jiayuhan.teachingservice.entity.TeachingOutboxEvent;
import xyz.jiayuhan.teachingservice.mapper.TeachingOutboxEventMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 在行锁保护下可靠派发教学服务审计事件。 */
@Service
@RequiredArgsConstructor
public class TeachingOutboxDispatchService {

    private final TeachingOutboxEventMapper outboxEventMapper;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${app.mq.outbox.batch-size:50}")
    private int batchSize;
    @Value("${app.mq.outbox.max-attempts:10}")
    private int maxAttempts;
    @Value("${app.mq.outbox.retention-days:7}")
    private int retentionDays;

    @Transactional(rollbackFor = Exception.class)
    public void publishPendingBatch() {
        for (TeachingOutboxEvent outbox : outboxEventMapper.selectPendingBatchForUpdate(batchSize)) {
            try {
                AuditLogEvent event = objectMapper.readValue(outbox.getPayload(), AuditLogEvent.class);
                eventPublisher.publish(outbox.getRoutingKey(), event);
                markPublished(outbox);
            } catch (Exception exception) {
                markFailedAttempt(outbox, exception);
            }
            outboxEventMapper.updateById(outbox);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int cleanupPublishedHistory() {
        return outboxEventMapper.deletePublishedBefore(
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(Math.max(1, retentionDays)));
    }

    private void markPublished(TeachingOutboxEvent outbox) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        outbox.setStatus("PUBLISHED");
        outbox.setPublishedAt(now);
        outbox.setUpdatedAt(now);
        outbox.setLastError(null);
    }

    private void markFailedAttempt(TeachingOutboxEvent outbox, Exception exception) {
        int retryCount = outbox.getRetryCount() + 1;
        outbox.setRetryCount(retryCount);
        outbox.setLastError(abbreviate(exception.getMessage()));
        outbox.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (retryCount >= maxAttempts) {
            outbox.setStatus("FAILED");
            return;
        }
        long delaySeconds = Math.min(300L, 1L << Math.min(retryCount, 8));
        outbox.setStatus("PENDING");
        outbox.setNextRetryAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds));
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "未知发布异常";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
