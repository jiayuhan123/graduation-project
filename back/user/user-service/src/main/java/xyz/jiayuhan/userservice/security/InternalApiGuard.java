package xyz.jiayuhan.userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userservice.config.InternalApiProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 用常量时间比较保护仅供受信任内部调用方使用的用户目录接口。 */
@Component
@RequiredArgsConstructor
public class InternalApiGuard {

    private final InternalApiProperties properties;

    public void check(String suppliedToken) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getToken())) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "内部用户目录接口未启用");
        }
        byte[] expected = properties.getToken().getBytes(StandardCharsets.UTF_8);
        byte[] supplied = suppliedToken == null
                ? new byte[0]
                : suppliedToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, supplied)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
