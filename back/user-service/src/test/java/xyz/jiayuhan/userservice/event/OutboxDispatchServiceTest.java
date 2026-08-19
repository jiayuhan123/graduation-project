package xyz.jiayuhan.userservice.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;
import xyz.jiayuhan.common.mq.publisher.DomainEventPublisher;
import xyz.jiayuhan.userservice.entity.OutboxEvent;
import xyz.jiayuhan.userservice.mapper.OutboxEventMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证发件箱发布成功与失败退避状态转换。 */
@ExtendWith(MockitoExtension.class)
class OutboxDispatchServiceTest {

    @Mock
    private OutboxEventMapper outboxEventMapper;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new OutboxDispatchService(outboxEventMapper, eventPublisher, objectMapper);
        ReflectionTestUtils.setField(dispatchService, "batchSize", 10);
        ReflectionTestUtils.setField(dispatchService, "maxAttempts", 3);
        ReflectionTestUtils.setField(dispatchService, "retentionDays", 7);
    }

    @Test
    void shouldMarkEventPublishedAfterBrokerConfirm() {
        OutboxEvent outbox = pendingOutbox();
        UserRegisteredEvent event = event();
        when(outboxEventMapper.selectPendingBatchForUpdate(10)).thenReturn(List.of(outbox));
        when(objectMapper.readValue("{}", UserRegisteredEvent.class)).thenReturn(event);

        dispatchService.publishPendingBatch();

        verify(eventPublisher).publish("user.registered", event);
        verify(outboxEventMapper).updateById(outbox);
        assertThat(outbox.getStatus()).isEqualTo("PUBLISHED");
        assertThat(outbox.getPublishedAt()).isNotNull();
        assertThat(outbox.getLastError()).isNull();
    }

    @Test
    void shouldScheduleRetryWhenPublishFails() {
        OutboxEvent outbox = pendingOutbox();
        UserRegisteredEvent event = event();
        LocalDateTime beforeDispatch = LocalDateTime.now();
        when(outboxEventMapper.selectPendingBatchForUpdate(10)).thenReturn(List.of(outbox));
        when(objectMapper.readValue("{}", UserRegisteredEvent.class)).thenReturn(event);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(eventPublisher).publish("user.registered", event);

        dispatchService.publishPendingBatch();

        assertThat(outbox.getStatus()).isEqualTo("PENDING");
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getNextRetryAt()).isAfter(beforeDispatch);
        assertThat(outbox.getLastError()).isEqualTo("broker unavailable");
        verify(outboxEventMapper).updateById(outbox);
    }

    @Test
    void shouldCleanupOnlyPublishedHistoryThroughMapper() {
        when(outboxEventMapper.deletePublishedBefore(any(LocalDateTime.class))).thenReturn(5);

        int deleted = dispatchService.cleanupPublishedHistory();

        assertThat(deleted).isEqualTo(5);
        verify(outboxEventMapper).deletePublishedBefore(any(LocalDateTime.class));
    }

    private OutboxEvent pendingOutbox() {
        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
        outbox.setEventType(UserRegisteredEvent.TYPE);
        outbox.setRoutingKey("user.registered");
        outbox.setPayload("{}");
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(LocalDateTime.now());
        outbox.setCreatedAt(LocalDateTime.now());
        return outbox;
    }

    private UserRegisteredEvent event() {
        return new UserRegisteredEvent(
                UUID.fromString("00000000-0000-4000-8000-000000000003"),
                UserRegisteredEvent.TYPE,
                Instant.parse("2026-08-19T12:00:00Z"),
                "user-service",
                42L,
                "demo",
                null);
    }
}
