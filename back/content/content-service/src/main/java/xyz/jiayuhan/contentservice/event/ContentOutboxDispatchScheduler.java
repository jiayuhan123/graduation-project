package xyz.jiayuhan.contentservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 内容服务发件箱调度入口。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentOutboxDispatchScheduler {

    private final ContentOutboxDispatchService dispatchService;

    @Scheduled(fixedDelayString = "${app.mq.outbox.fixed-delay-ms:5000}")
    public void dispatch() {
        try {
            dispatchService.publishPendingBatch();
        } catch (Exception exception) {
            log.error("内容服务事务发件箱派发失败", exception);
        }
    }

    @Scheduled(cron = "${app.mq.outbox.cleanup-cron:0 0 3 * * *}")
    public void cleanupPublishedHistory() {
        try {
            int deleted = dispatchService.cleanupPublishedHistory();
            if (deleted > 0) {
                log.info("已清理内容服务历史发件箱记录：count={}", deleted);
            }
        } catch (Exception exception) {
            log.error("清理内容服务历史发件箱记录失败", exception);
        }
    }
}
