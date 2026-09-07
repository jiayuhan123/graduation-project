package xyz.jiayuhan.learningservice.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import xyz.jiayuhan.learningservice.entity.AssignmentSyncEvent;
import xyz.jiayuhan.learningservice.mapper.AssignmentSyncEventMapper;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentProgressDispatchServiceTest {

    @Mock private AssignmentSyncEventMapper eventMapper;
    @Mock private TeachingFeignClient teachingFeignClient;
    @InjectMocks private AssignmentProgressDispatchService service;

    @Test
    void keepsEventPendingAndSchedulesRetryWhenTeachingServiceFails() {
        AssignmentSyncEvent event = new AssignmentSyncEvent();
        event.setId(1L);
        event.setAssignmentId(2L);
        event.setStudentId(3L);
        event.setPracticeTaskId(4L);
        event.setProgressStatus("IN_PROGRESS");
        event.setDeliveryStatus("PENDING");
        event.setRetryCount(0);
        event.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        when(eventMapper.selectPendingBatchForUpdate(10)).thenReturn(List.of(event));
        when(teachingFeignClient.syncAssignmentProgress(eq(2L), eq(3L), any()))
                .thenThrow(new IllegalStateException("connection refused"));
        ReflectionTestUtils.setField(service, "batchSize", 10);

        service.dispatchPendingBatch();

        assertThat(event.getDeliveryStatus()).isEqualTo("PENDING");
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).contains("connection refused");
        assertThat(event.getNextRetryAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
        verify(eventMapper).updateById(event);
    }
}
