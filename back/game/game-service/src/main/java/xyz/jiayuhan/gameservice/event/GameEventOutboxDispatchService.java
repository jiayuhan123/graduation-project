package xyz.jiayuhan.gameservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.mq.publisher.DomainEventPublisher;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.common.mq.event.GameLearningEvent;
import xyz.jiayuhan.gameservice.entity.GameEventOutbox;
import xyz.jiayuhan.gameservice.mapper.GameEventOutboxMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 使用跳过锁定的批量行锁可靠派发游戏事务发件箱。 */
@Service
@RequiredArgsConstructor
public class GameEventOutboxDispatchService {

    private final GameEventOutboxMapper outboxMapper;
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
        for (GameEventOutbox outbox : outboxMapper.selectPendingBatchForUpdate(batchSize)) {
            try {
                GameLearningEvent event = objectMapper.readValue(outbox.getPayload(), GameLearningEvent.class);
                eventPublisher.publish(routingKey(outbox.getEventType()), event);
                markSent(outbox);
            } catch (Exception exception) {
                markFailedAttempt(outbox, exception);
            }
            outboxMapper.updateById(outbox);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int cleanupSentHistory() {
        return outboxMapper.deleteSentBefore(
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(Math.max(1, retentionDays)));
    }

    private String routingKey(String eventType) {
        return switch (eventType) {
            case GameEventOutboxService.ANSWER_GRADED -> MqTopology.GAME_ANSWER_GRADED_ROUTING_KEY;
            case GameEventOutboxService.SESSION_COMPLETED -> MqTopology.GAME_SESSION_COMPLETED_ROUTING_KEY;
            default -> "game.event";
        };
    }

    private void markSent(GameEventOutbox outbox) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        outbox.setDeliveryStatus("SENT");
        outbox.setSentAt(now);
        outbox.setLastError(null);
    }

    private void markFailedAttempt(GameEventOutbox outbox, Exception exception) {
        int attempts = outbox.getRetryCount() + 1;
        outbox.setRetryCount(attempts);
        outbox.setLastError(abbreviate(exception.getMessage()));
        if (attempts >= maxAttempts) {
            outbox.setDeliveryStatus("DEAD");
            return;
        }
        long delaySeconds = Math.min(300L, 1L << Math.min(attempts, 8));
        outbox.setDeliveryStatus("PENDING");
        outbox.setNextRetryAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds));
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "未知发布异常";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
