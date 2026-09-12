package xyz.jiayuhan.monomer.security;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.jiayuhan.common.security.context.UserContext;

/** 在单体入口完成会话校验，并建立原业务模块继续使用的用户上下文。 */
@Component
public class MonolithAuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        StpUtil.checkLogin();
        UserContext.setUserId(StpUtil.getLoginIdAsLong());
        return true;
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
