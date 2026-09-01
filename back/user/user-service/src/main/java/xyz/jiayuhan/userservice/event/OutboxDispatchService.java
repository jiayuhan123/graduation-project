package xyz.jiayuhan.userservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;
import xyz.jiayuhan.common.mq.event.DomainEvent;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;
import xyz.jiayuhan.common.mq.event.UserLifecycleEvent;
import xyz.jiayuhan.common.mq.publisher.DomainEventPublisher;
import xyz.jiayuhan.userservice.entity.OutboxEvent;
import xyz.jiayuhan.userservice.mapper.OutboxEventMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 在数据库行锁保护下批量派发用户服务发件箱事件。 */
@Service
@RequiredArgsConstructor
public class OutboxDispatchService {

    private static final String PENDING = "PENDING";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String FAILED = "FAILED";

    private final OutboxEventMapper outboxEventMapper;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${app.mq.outbox.batch-size:50}")
    private int batchSize;

    @Value("${app.mq.outbox.max-attempts:10}")
    private int maxAttempts;

    @Value("${app.mq.outbox.retention-days:7}")
    private int retentionDays;

    /**
     * 发布一批到期事件。
     *
     * <p>RabbitMQ 已确认但数据库提交失败时可能产生重复消息，所以消费者仍必须按 eventId 幂等。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishPendingBatch() {
        for (OutboxEvent outbox : outboxEventMapper.selectPendingBatchForUpdate(batchSize)) {
            try {
                DomainEvent event = deserialize(outbox);
                eventPublisher.publish(outbox.getRoutingKey(), event);
                markPublished(outbox);
            } catch (Exception exception) {
                markFailedAttempt(outbox, exception);
            }
            outboxEventMapper.updateById(outbox);
        }
    }

    /** 清理成功发布的历史记录；失败记录始终保留，等待运维判断和重放。 */
    @Transactional(rollbackFor = Exception.class)
    public int cleanupPublishedHistory() {
        int safeRetentionDays = Math.max(1, retentionDays);
        return outboxEventMapper.deletePublishedBefore(
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(safeRetentionDays));
    }

    private DomainEvent deserialize(OutboxEvent outbox) {
        return switch (outbox.getEventType()) {
            case UserRegisteredEvent.TYPE ->
                    objectMapper.readValue(outbox.getPayload(), UserRegisteredEvent.class);
            case UserLifecycleEvent.TYPE ->
                    objectMapper.readValue(outbox.getPayload(), UserLifecycleEvent.class);
            case AuditLogEvent.TYPE ->
                    objectMapper.readValue(outbox.getPayload(), AuditLogEvent.class);
            default -> throw new IllegalArgumentException("不支持的发件箱事件类型：" + outbox.getEventType());
        };
    }

    private void markPublished(OutboxEvent outbox) {
        outbox.setStatus(PUBLISHED);
        outbox.setPublishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        outbox.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        outbox.setLastError(null);
    }

    private void markFailedAttempt(OutboxEvent outbox, Exception exception) {
        int retryCount = outbox.getRetryCount() + 1;
        outbox.setRetryCount(retryCount);
        outbox.setLastError(abbreviate(exception.getMessage()));
        outbox.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (retryCount >= maxAttempts) {
            outbox.setStatus(FAILED);
            return;
        }
        // 指数退避最大五分钟，避免 MQ 故障期间形成高频重试风暴。
        long delaySeconds = Math.min(300L, 1L << Math.min(retryCount, 8));
        outbox.setStatus(PENDING);
        outbox.setNextRetryAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds));
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "未知发布异常";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
