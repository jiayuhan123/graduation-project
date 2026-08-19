package xyz.jiayuhan.common.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.security.SecurityHeaders;
import xyz.jiayuhan.common.security.context.UserContext;

/** 解析网关写入的用户 ID，并在请求生命周期内维护 {@link UserContext}。 */
public class GatewayUserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(SecurityHeaders.USER_ID);
        if (!StringUtils.hasText(userId)) {
            return true;
        }
        try {
            UserContext.setUserId(Long.valueOf(userId));
            return true;
        } catch (NumberFormatException exception) {
            // 网关生成的身份头只可能是 Long；格式错误说明请求没有遵守内部协议。
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户身份请求头格式不正确");
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        UserContext.clear();
    }
}
