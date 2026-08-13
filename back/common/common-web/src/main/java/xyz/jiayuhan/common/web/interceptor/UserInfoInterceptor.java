package xyz.jiayuhan.common.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.jiayuhan.common.web.context.UserContext;

/**
 * 统一获取并解析网关透传的 X-User-ID 的拦截器
 */
public class UserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头获取网关塞入的 X-User-ID
        String userIdStr = request.getHeader("X-User-ID");
        if (StringUtils.hasText(userIdStr)) {
            try {
                Long userId = Long.parseLong(userIdStr);
                // 存入当前线程上下文
                UserContext.setUserId(userId);
            } catch (NumberFormatException e) {
                // 解析失败，忽略或记录日志
            }
        }
        // 无论有没有拿到 userId，都放行。权限校验已在网关完成，本拦截器仅负责提取信息。
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后，必须清理 ThreadLocal，防止内存泄漏或线程池复用导致的数据串读
        UserContext.removeUserId();
    }
}
