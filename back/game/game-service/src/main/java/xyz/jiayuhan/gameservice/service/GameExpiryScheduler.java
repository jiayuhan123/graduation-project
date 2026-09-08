package xyz.jiayuhan.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时收口超时等待、超时答题和过期随机匹配。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameExpiryScheduler {

    private final GameSessionTransactionService transactionService;

    @Scheduled(fixedDelayString = "${app.game.expiry.fixed-delay-ms:5000}")
    public void expireDueGames() {
        try {
            int count = transactionService.expireDueSessions(50);
            if (count > 0) {
                log.info("已收口超时游戏对局：count={}", count);
            }
        } catch (Exception exception) {
            log.error("收口超时游戏对局失败", exception);
        }
    }
}
