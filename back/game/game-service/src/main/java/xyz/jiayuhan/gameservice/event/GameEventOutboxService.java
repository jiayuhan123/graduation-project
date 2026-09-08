package xyz.jiayuhan.gameservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.mq.event.GameLearningEvent;
import xyz.jiayuhan.gameservice.entity.GameEventOutbox;
import xyz.jiayuhan.gameservice.mapper.GameEventOutboxMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

/** 在游戏写事务内记录可重试的答案与结算事件。 */
@Service
@RequiredArgsConstructor
public class GameEventOutboxService {

    public static final String ANSWER_GRADED = "game.answer.graded.v1";
    public static final String SESSION_COMPLETED = "game.session.completed.v1";

    private final GameEventOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(
            String eventKey,
            String eventType,
            String aggregateType,
            Long aggregateId,
            Long studentId,
            Map<String, Object> data) {
        GameLearningEvent event = new GameLearningEvent(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                "game-service",
                aggregateType,
                aggregateId,
                studentId,
                Map.copyOf(data));
        GameEventOutbox outbox = new GameEventOutbox();
        outbox.setEventKey(eventKey);
        outbox.setEventType(eventType);
        outbox.setAggregateType(aggregateType);
        outbox.setAggregateId(aggregateId);
        outbox.setStudentId(studentId);
        outbox.setPayload(objectMapper.writeValueAsString(event));
        outbox.setDeliveryStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(OffsetDateTime.now(ZoneOffset.UTC));
        outboxMapper.insert(outbox);
    }
}
