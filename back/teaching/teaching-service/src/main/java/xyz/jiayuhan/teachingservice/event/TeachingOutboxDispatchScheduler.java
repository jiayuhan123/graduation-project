package xyz.jiayuhan.teachingservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 周期派发并清理教学审计发件箱。 */
@Component
@RequiredArgsConstructor
public class TeachingOutboxDispatchScheduler {

    private final TeachingOutboxDispatchService dispatchService;

    @Scheduled(fixedDelayString = "${app.mq.outbox.fixed-delay-ms:5000}")
    public void publishPending() {
        dispatchService.publishPendingBatch();
    }

    @Scheduled(cron = "${app.mq.outbox.cleanup-cron:0 0 3 * * *}")
    public void cleanupPublished() {
        dispatchService.cleanupPublishedHistory();
    }
}
