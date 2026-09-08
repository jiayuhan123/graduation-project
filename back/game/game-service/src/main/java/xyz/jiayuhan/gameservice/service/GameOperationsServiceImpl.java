package xyz.jiayuhan.gameservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.gameservice.entity.GameSession;
import xyz.jiayuhan.gameservice.mapper.GameSessionMapper;
import xyz.jiayuhan.gameservice.model.response.GameOperationsStatisticsResponse;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 从服务本地对局事实生成 PK 完成和异常指标。 */
@Service
@RequiredArgsConstructor
public class GameOperationsServiceImpl implements GameOperationsService {

    private final GameSessionMapper gameSessionMapper;
    private final UserFeignClient userFeignClient;

    @Override
    public GameOperationsStatisticsResponse statistics(Long requesterId) {
        requireAdmin(requesterId);
        long total = countByStatuses(null);
        long active = countByStatuses(List.of("WAITING", "READY", "IN_PROGRESS"));
        long completed = countByStatuses(List.of("COMPLETED"));
        long cancelled = countByStatuses(List.of("CANCELLED"));
        long expired = countByStatuses(List.of("EXPIRED"));
        long abnormal = cancelled + expired;
        return new GameOperationsStatisticsResponse(
                total, active, completed, cancelled, expired, abnormal,
                ratio(completed, total), ratio(abnormal, total));
    }

    private long countByStatuses(List<String> statuses) {
        return gameSessionMapper.selectCount(new LambdaQueryWrapper<GameSession>()
                .eq(GameSession::getMode, "PK")
                .in(statuses != null, GameSession::getStatus, statuses));
    }

    private void requireAdmin(Long requesterId) {
        Result<UserSummaryResponse> result;
        try {
            result = userFeignClient.getUserSummary(requesterId);
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "暂时无法校验管理员身份");
        }
        if (result == null || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null || result.getData().roles() == null
                || !result.getData().roles().contains("admin")
                || !Integer.valueOf(1).equals(result.getData().status())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private BigDecimal ratio(long numerator, long denominator) {
        return denominator == 0
                ? BigDecimal.ZERO.setScale(4)
                : BigDecimal.valueOf(numerator)
                        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
