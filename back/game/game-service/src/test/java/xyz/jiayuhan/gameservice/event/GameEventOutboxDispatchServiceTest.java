package xyz.jiayuhan.gameservice.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import xyz.jiayuhan.common.mq.publisher.DomainEventPublisher;
import xyz.jiayuhan.gameservice.entity.GameEventOutbox;
import xyz.jiayuhan.gameservice.mapper.GameEventOutboxMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameEventOutboxDispatchServiceTest {

    @Mock private GameEventOutboxMapper outboxMapper;
    @Mock private DomainEventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private GameEventOutboxDispatchService service;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        service = new GameEventOutboxDispatchService(outboxMapper, eventPublisher, objectMapper);
    }

    @Test
    void marksEventSentOnlyAfterPublisherReturns() {
        GameDomainEvent event = new GameDomainEvent(
                UUID.randomUUID(),
                GameEventOutboxService.ANSWER_GRADED,
                Instant.now(),
                "game-service",
                "ANSWER",
                11L,
                21L,
                Map.of("correct", true));
        GameEventOutbox outbox = new GameEventOutbox();
        outbox.setId(1L);
        outbox.setEventType(event.eventType());
        outbox.setPayload(objectMapper.writeValueAsString(event));
        outbox.setDeliveryStatus("PENDING");
        outbox.setRetryCount(0);
        when(outboxMapper.selectPendingBatchForUpdate(0)).thenReturn(List.of(outbox));
        when(outboxMapper.updateById(outbox)).thenReturn(1);

        service.publishPendingBatch();

        verify(eventPublisher).publish(eq("game.answer.graded"), any(GameDomainEvent.class));
        assertThat(outbox.getDeliveryStatus()).isEqualTo("SENT");
        assertThat(outbox.getSentAt()).isNotNull();
    }
}
