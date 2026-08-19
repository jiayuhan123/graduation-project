package xyz.jiayuhan.userservice.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userservice.service.LoginAttemptService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Redis 登录失败计数实现。
 *
 * <p>计数窗口从第一次失败开始，达到阈值后在窗口剩余时间内拒绝继续尝试；
 * Redis Key 使用账号摘要，避免运维侧直接看到手机号或邮箱等登录标识。</p>
 */
@Service
public class RedisLoginAttemptService implements LoginAttemptService {

    private static final String KEY_PREFIX = "user:login:failure:";

    private final StringRedisTemplate redisTemplate;
    private final int maxAttempts;
    private final Duration lockDuration;

    /**
     * 创建登录失败计数器。
     *
     * @param redisTemplate Redis 字符串操作模板
     * @param maxAttempts 一个计数窗口内允许的最大失败次数
     * @param lockDuration 失败计数及锁定窗口时长
     */
    public RedisLoginAttemptService(
            StringRedisTemplate redisTemplate,
            @Value("${security.login-attempt.max-attempts:5}") int maxAttempts,
            @Value("${security.login-attempt.lock-duration:15m}") Duration lockDuration) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
    }

    @Override
    public void checkAllowed(String account) {
        String value = redisTemplate.opsForValue().get(key(account));
        if (value != null && Long.parseLong(value) >= maxAttempts) {
            throw new BusinessException(ResultCode.LOGIN_TOO_FREQUENT);
        }
    }

    @Override
    public void recordFailure(String account) {
        String key = key(account);
        Long failures = redisTemplate.opsForValue().increment(key);
        // 只在首次失败时设置 TTL，避免持续尝试不断延长窗口造成永久锁定。
        if (failures != null && failures == 1L) {
            redisTemplate.expire(key, lockDuration);
        }
    }

    @Override
    public void clear(String account) {
        redisTemplate.delete(key(account));
    }

    private String key(String account) {
        try {
            // SHA-256 只用于隐藏 Redis Key 中的账号明文，不承担密码存储职责。
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(account.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }
}
