package xyz.jiayuhan.auditservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.auditservice.entity.AuditLog;
import xyz.jiayuhan.auditservice.mapper.AuditLogMapper;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证审计事件入库映射和基于 eventId 的消费幂等。 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void shouldStoreNewAuditEvent() {
        AuditLogEvent event = event();
        when(auditLogMapper.exists(any())).thenReturn(false);

        auditLogService.record(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(event.eventId());
        assertThat(saved.getOperatorId()).isEqualTo(7L);
        assertThat(saved.getAction()).isEqualTo("USER_STATUS_CHANGED");
        assertThat(saved.getTargetId()).isEqualTo("42");
    }

    @Test
    void shouldIgnoreDuplicateAuditEvent() {
        when(auditLogMapper.exists(any())).thenReturn(true);

        auditLogService.record(event());

        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    private AuditLogEvent event() {
        return new AuditLogEvent(
                UUID.fromString("00000000-0000-4000-8000-000000000002"),
                AuditLogEvent.TYPE,
                Instant.parse("2026-08-19T12:00:00Z"),
                "user-service",
                7L,
                "USER_STATUS_CHANGED",
                "USER",
                "42",
                "enabled=false");
    }
}
