package xyz.jiayuhan.userservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.jiayuhan.userapi.UserApiConstants;

/** 统一认证所有用户服务内部 HTTP 请求，避免 Controller 遗漏鉴权。 */
@Component
@RequiredArgsConstructor
public class InternalApiAuthenticationInterceptor implements HandlerInterceptor {

    private final InternalApiGuard internalApiGuard;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        internalApiGuard.check(request.getHeader(UserApiConstants.INTERNAL_TOKEN_HEADER));
        return true;
    }
}
