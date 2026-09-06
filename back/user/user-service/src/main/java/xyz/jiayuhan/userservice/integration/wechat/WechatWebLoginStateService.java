package xyz.jiayuhan.userservice.integration.wechat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import java.security.SecureRandom;
import java.util.Base64;

/** 保存并一次性消费微信网页 OAuth state，同时通过 Cookie 把 state 绑定到发起登录的浏览器。 */
@Service
@RequiredArgsConstructor
public class WechatWebLoginStateService {

    private static final String KEY_PREFIX = "user:wechat:web-login-state:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WechatWebProperties properties;

    public String create(WechatWebLoginContext context) {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + state,
                    objectMapper.writeValueAsString(context),
                    properties.getStateTtl());
            return state;
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "无法创建微信登录会话");
        }
    }

    public WechatWebLoginContext consume(String state, String cookieState) {
        if (!StringUtils.hasText(state) || !state.equals(cookieState)) {
            throw new BusinessException(ResultCode.WECHAT_CODE_INVALID, "微信登录状态无效或已过期");
        }
        String value = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + state);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.WECHAT_CODE_INVALID, "微信登录状态无效或已过期");
        }
        try {
            return objectMapper.readValue(value, WechatWebLoginContext.class);
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.WECHAT_CODE_INVALID, "微信登录状态无效或已过期");
        }
    }
}
