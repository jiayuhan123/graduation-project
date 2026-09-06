package xyz.jiayuhan.contentservice.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;
import xyz.jiayuhan.common.mq.publisher.DomainEventPublisher;
import xyz.jiayuhan.contentservice.entity.ContentOutboxEvent;
import xyz.jiayuhan.contentservice.mapper.ContentOutboxEventMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证内容审计发件箱的发布确认、退避重试和终止重试。 */
@ExtendWith(MockitoExtension.class)
class ContentOutboxDispatchServiceTest {

    @Mock
    private ContentOutboxEventMapper outboxEventMapper;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private ObjectMapper objectMapper;

    private ContentOutboxDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new ContentOutboxDispatchService(outboxEventMapper, eventPublisher, objectMapper);
        ReflectionTestUtils.setField(dispatchService, "batchSize", 10);
        ReflectionTestUtils.setField(dispatchService, "maxAttempts", 3);
        ReflectionTestUtils.setField(dispatchService, "retentionDays", 7);
    }

    @Test
    void marksEventPublishedOnlyAfterPublisherReturns() {
        ContentOutboxEvent outbox = pendingOutbox(0);
        AuditLogEvent event = auditEvent();
        when(outboxEventMapper.selectPendingBatchForUpdate(10)).thenReturn(List.of(outbox));
        when(objectMapper.readValue("{}", AuditLogEvent.class)).thenReturn(event);

        dispatchService.publishPendingBatch();

        verify(eventPublisher).publish("audit.operation", event);
        verify(outboxEventMapper).updateById(outbox);
        assertThat(outbox.getStatus()).isEqualTo("PUBLISHED");
        assertThat(outbox.getPublishedAt()).isNotNull();
        assertThat(outbox.getLastError()).isNull();
    }

    @Test
    void schedulesRetryBeforeAttemptLimit() {
        ContentOutboxEvent outbox = pendingOutbox(0);
        AuditLogEvent event = auditEvent();
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        when(outboxEventMapper.selectPendingBatchForUpdate(10)).thenReturn(List.of(outbox));
        when(objectMapper.readValue("{}", AuditLogEvent.class)).thenReturn(event);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(eventPublisher).publish("audit.operation", event);

        dispatchService.publishPendingBatch();

        assertThat(outbox.getStatus()).isEqualTo("PENDING");
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getNextRetryAt()).isAfter(startedAt);
        assertThat(outbox.getLastError()).isEqualTo("broker unavailable");
    }

    @Test
    void marksEventFailedAtAttemptLimit() {
        ContentOutboxEvent outbox = pendingOutbox(2);
        when(outboxEventMapper.selectPendingBatchForUpdate(10)).thenReturn(List.of(outbox));
        when(objectMapper.readValue("{}", AuditLogEvent.class))
                .thenThrow(new IllegalArgumentException("invalid payload"));

        dispatchService.publishPendingBatch();

        assertThat(outbox.getStatus()).isEqualTo("FAILED");
        assertThat(outbox.getRetryCount()).isEqualTo(3);
        assertThat(outbox.getLastError()).isEqualTo("invalid payload");
        verify(outboxEventMapper).updateById(outbox);
    }

    private ContentOutboxEvent pendingOutbox(int retryCount) {
        ContentOutboxEvent outbox = new ContentOutboxEvent();
        outbox.setId(UUID.fromString("00000000-0000-4000-8000-000000000021"));
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
                UUID.fromString("00000000-0000-4000-8000-000000000021"),
                AuditLogEvent.TYPE,
                Instant.parse("2026-09-06T00:00:00Z"),
                "content-service",
                42L,
                "content.question.create",
                "question",
                "100",
                "code=Q001");
    }
}
