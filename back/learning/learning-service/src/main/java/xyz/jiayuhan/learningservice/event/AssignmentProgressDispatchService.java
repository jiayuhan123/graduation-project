package xyz.jiayuhan.learningservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.learningservice.entity.AssignmentSyncEvent;
import xyz.jiayuhan.learningservice.mapper.AssignmentSyncEventMapper;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.teachingapi.model.request.AssignmentProgressRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 按任务顺序派发作业进度；远程失败时保留事件并指数退避。 */
@Service
@RequiredArgsConstructor
public class AssignmentProgressDispatchService {

    private final AssignmentSyncEventMapper eventMapper;
    private final TeachingFeignClient teachingFeignClient;

    @Value("${app.assignment-sync.batch-size:50}")
    private int batchSize;
    @Value("${app.assignment-sync.retention-days:7}")
    private int retentionDays;

    @Transactional(rollbackFor = Exception.class)
    public void dispatchPendingBatch() {
        for (AssignmentSyncEvent event : eventMapper.selectPendingBatchForUpdate(batchSize)) {
            try {
                Result<Void> result = teachingFeignClient.syncAssignmentProgress(
                        event.getAssignmentId(),
                        event.getStudentId(),
                        new AssignmentProgressRequest(
                                event.getPracticeTaskId(), event.getProgressStatus(), event.getScore(),
                                event.getCorrectCount(), event.getTotalCount(), event.getStartedAt(),
                                event.getSubmittedAt(), event.getGradedAt()));
                if (result == null || !Integer.valueOf(200).equals(result.getCode())) {
                    throw new IllegalStateException("教学服务拒绝同步，响应码="
                            + (result == null ? "null" : result.getCode()));
                }
                markSent(event);
            } catch (Exception exception) {
                markRetry(event, exception);
            }
            if (eventMapper.updateById(event) != 1) {
                throw new IllegalStateException("作业进度同步事件状态更新失败: " + event.getId());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int cleanupSentHistory() {
        return eventMapper.deleteSentBefore(
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(Math.max(1, retentionDays)));
    }

    private void markSent(AssignmentSyncEvent event) {
        OffsetDateTime current = OffsetDateTime.now(ZoneOffset.UTC);
        event.setDeliveryStatus("SENT");
        event.setSyncedAt(current);
        event.setLastError(null);
    }

    private void markRetry(AssignmentSyncEvent event, Exception exception) {
        int retryCount = event.getRetryCount() + 1;
        long delaySeconds = Math.min(300L, 1L << Math.min(retryCount, 8));
        event.setRetryCount(retryCount);
        event.setNextRetryAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds));
        event.setLastError(abbreviate(exception.getMessage()));
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "未知作业同步异常";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
