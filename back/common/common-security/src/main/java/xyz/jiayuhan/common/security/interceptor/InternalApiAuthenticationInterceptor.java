package xyz.jiayuhan.common.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.security.SecurityHeaders;
import xyz.jiayuhan.common.security.config.InternalApiProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 统一保护 {@code /internal/**}，防止内部契约被当作公开 API 调用。 */
public class InternalApiAuthenticationInterceptor implements HandlerInterceptor {

    private final InternalApiProperties properties;

    public InternalApiAuthenticationInterceptor(InternalApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getToken())) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "内部接口未启用");
        }
        String suppliedToken = request.getHeader(SecurityHeaders.INTERNAL_TOKEN);
        byte[] expected = properties.getToken().getBytes(StandardCharsets.UTF_8);
        byte[] supplied = suppliedToken == null
                ? new byte[0]
                : suppliedToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, supplied)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return true;
    }
}
