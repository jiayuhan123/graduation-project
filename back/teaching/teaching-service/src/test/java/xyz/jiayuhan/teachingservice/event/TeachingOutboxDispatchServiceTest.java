package xyz.jiayuhan.teachingservice.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;
import xyz.jiayuhan.common.mq.publisher.DomainEventPublisher;
import xyz.jiayuhan.teachingservice.entity.TeachingOutboxEvent;
import xyz.jiayuhan.teachingservice.mapper.TeachingOutboxEventMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证教学审计发件箱只在确认投递后完成，并按次数退避重试。 */
@ExtendWith(MockitoExtension.class)
class TeachingOutboxDispatchServiceTest {

    @Mock
    private TeachingOutboxEventMapper outboxEventMapper;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private ObjectMapper objectMapper;

    private TeachingOutboxDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new TeachingOutboxDispatchService(
                outboxEventMapper, eventPublisher, objectMapper);
        ReflectionTestUtils.setField(dispatchService, "batchSize", 10);
        ReflectionTestUtils.setField(dispatchService, "maxAttempts", 3);
        ReflectionTestUtils.setField(dispatchService, "retentionDays", 7);
    }

    @Test
    void marksEventPublishedAfterPublisherReturns() {
        TeachingOutboxEvent outbox = pendingOutbox(0);
        AuditLogEvent event = auditEvent();
        when(outboxEventMapper.selectPendingBatchForUpdate(10)).thenReturn(List.of(outbox));
        when(objectMapper.readValue("{}", AuditLogEvent.class)).thenReturn(event);

        dispatchService.publishPendingBatch();

        verify(eventPublisher).publish("audit.operation", event);
        verify(outboxEventMapper).updateById(outbox);
        assertThat(outbox.getStatus()).isEqualTo("PUBLISHED");
        assertThat(outbox.getPublishedAt()).isNotNull();
    }

    @Test
    void retriesBeforeAttemptLimitAndFailsAtLimit() {
        TeachingOutboxEvent retrying = pendingOutbox(0);
        TeachingOutboxEvent exhausted = pendingOutbox(2);
        AuditLogEvent event = auditEvent();
        when(outboxEventMapper.selectPendingBatchForUpdate(10))
                .thenReturn(List.of(retrying), List.of(exhausted));
        when(objectMapper.readValue("{}", AuditLogEvent.class)).thenReturn(event);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(eventPublisher).publish("audit.operation", event);

        dispatchService.publishPendingBatch();
        dispatchService.publishPendingBatch();

        assertThat(retrying.getStatus()).isEqualTo("PENDING");
        assertThat(retrying.getRetryCount()).isEqualTo(1);
        assertThat(retrying.getNextRetryAt()).isAfter(retrying.getCreatedAt());
        assertThat(exhausted.getStatus()).isEqualTo("FAILED");
        assertThat(exhausted.getRetryCount()).isEqualTo(3);
    }

    private TeachingOutboxEvent pendingOutbox(int retryCount) {
        TeachingOutboxEvent outbox = new TeachingOutboxEvent();
        outbox.setId(UUID.fromString("00000000-0000-4000-8000-000000000031"));
        outbox.setEventType(AuditLogEvent.TYPE);
        outbox.setRoutingKey("audit.operation");
        outbox.setPayload("{}");
        outbox.setStatus("PENDING");
        outbox.setRetryCount(retryCount);
        outbox.setNextRetryAt(OffsetDateTime.now(ZoneOffset.UTC));
        outbox.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return outbox;
    }

    private AuditLogEvent auditEvent() {
        return new AuditLogEvent(
                UUID.fromString("00000000-0000-4000-8000-000000000031"),
                AuditLogEvent.TYPE,
                Instant.parse("2026-09-06T00:00:00Z"),
                "teaching-service",
                42L,
                "teaching.assignment.publish",
                "assignment",
                "100",
                "studentCount=2");
    }
}
