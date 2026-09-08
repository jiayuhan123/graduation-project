package xyz.jiayuhan.gameservice.model.response;

import java.math.BigDecimal;

/** 管理员运营看板的双人 PK 指标。 */
public record GameOperationsStatisticsResponse(
        long totalPkSessions,
        long activePkSessions,
        long completedPkSessions,
        long cancelledPkSessions,
        long expiredPkSessions,
        long abnormalPkSessions,
        BigDecimal completionRate,
        BigDecimal abnormalRate
) {
}
