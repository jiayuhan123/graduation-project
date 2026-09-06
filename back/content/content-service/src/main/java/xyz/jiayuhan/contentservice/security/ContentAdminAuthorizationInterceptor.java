package xyz.jiayuhan.contentservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;

/** 内容管理接口的跨服务权限守卫。 */
@Component
@RequiredArgsConstructor
public class ContentAdminAuthorizationInterceptor implements HandlerInterceptor {

    public static final String CONTENT_MANAGE_PERMISSION = "content:manage";

    private final UserFeignClient userFeignClient;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        Long userId = UserContext.requireUserId();
        Result<UserPermissionResponse> result =
                userFeignClient.authorizeUser(userId, CONTENT_MANAGE_PERMISSION);
        if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "暂时无法校验内容管理权限");
        }
        UserPermissionResponse authorization = result.getData();
        if (!authorization.exists() || !authorization.active() || !authorization.granted()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return true;
    }
}
