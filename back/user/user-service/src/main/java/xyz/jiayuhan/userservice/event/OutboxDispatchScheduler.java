package xyz.jiayuhan.userservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期触发事务发件箱派发；实际事务放在独立服务中，避免同类调用导致事务失效。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatchScheduler {

    private final OutboxDispatchService dispatchService;

    @Scheduled(fixedDelayString = "${app.mq.outbox.fixed-delay-ms:5000}")
    public void dispatch() {
        try {
            dispatchService.publishPendingBatch();
        } catch (Exception exception) {
            // 调度线程不能因一次基础设施故障退出，下一周期会继续领取未发布事件。
            log.error("用户服务事务发件箱派发失败", exception);
        }
    }

    /** 每天清理超过保留期的成功记录，防止高频事件长期占用热表空间。 */
    @Scheduled(cron = "${app.mq.outbox.cleanup-cron:0 0 3 * * *}")
    public void cleanupPublishedHistory() {
        try {
            int deleted = dispatchService.cleanupPublishedHistory();
            if (deleted > 0) {
                log.info("已清理用户服务历史发件箱记录：count={}", deleted);
            }
        } catch (Exception exception) {
            // 清理失败不能影响消息派发；下一个清理周期会继续尝试。
            log.error("清理用户服务历史发件箱记录失败", exception);
        }
    }
}
