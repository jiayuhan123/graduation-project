package xyz.jiayuhan.aiservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** 使用 Redis SETNX 保证一个确认令牌只执行一次。 */
@Service
public class AgentActionReplayGuard {

    private static final String KEY_PREFIX = "ai:confirmed-action:";
    private final StringRedisTemplate redisTemplate;

    public AgentActionReplayGuard(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void claim(UUID draftId, OffsetDateTime expiresAt) {
        Duration ttl = Duration.between(OffsetDateTime.now(ZoneOffset.UTC), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            throw new BusinessException(ResultCode.CONFLICT, "Agent 操作确认已过期");
        }
        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + draftId, "1", ttl);
        if (!Boolean.TRUE.equals(claimed)) {
            throw new BusinessException(ResultCode.CONFLICT, "该 Agent 操作已经确认或正在执行");
        }
    }

    public void release(UUID draftId) {
        redisTemplate.delete(KEY_PREFIX + draftId);
    }
}
