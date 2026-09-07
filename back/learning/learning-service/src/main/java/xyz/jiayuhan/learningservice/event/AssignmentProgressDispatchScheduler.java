package xyz.jiayuhan.learningservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 周期同步作业进度并清理已成功事件。 */
@Component
@RequiredArgsConstructor
public class AssignmentProgressDispatchScheduler {

    private final AssignmentProgressDispatchService dispatchService;

    @Scheduled(fixedDelayString = "${app.assignment-sync.fixed-delay-ms:3000}")
    public void dispatchPending() {
        dispatchService.dispatchPendingBatch();
    }

    @Scheduled(cron = "${app.assignment-sync.cleanup-cron:0 20 3 * * *}")
    public void cleanupSent() {
        dispatchService.cleanupSentHistory();
    }
}
