package xyz.jiayuhan.gameservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 游戏事务发件箱的定时派发与历史清理入口。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventOutboxScheduler {

    private final GameEventOutboxDispatchService dispatchService;

    @Scheduled(fixedDelayString = "${app.mq.outbox.fixed-delay-ms:5000}")
    public void dispatch() {
        try {
            dispatchService.publishPendingBatch();
        } catch (Exception exception) {
            log.error("游戏服务事务发件箱派发失败", exception);
        }
    }

    @Scheduled(cron = "${app.mq.outbox.cleanup-cron:0 20 3 * * *}")
    public void cleanup() {
        try {
            int deleted = dispatchService.cleanupSentHistory();
            if (deleted > 0) {
                log.info("已清理游戏服务历史发件箱记录：count={}", deleted);
            }
        } catch (Exception exception) {
            log.error("清理游戏服务历史发件箱失败", exception);
        }
    }
}
