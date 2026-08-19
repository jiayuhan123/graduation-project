package xyz.jiayuhan.common.security.context;

import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

/**
 * 当前请求用户上下文。
 *
 * <p>上下文由网关身份头拦截器写入并在请求完成后清理，只能在当前 Servlet 请求线程中使用。</p>
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /** 写入当前请求的用户 ID，仅供公共拦截器调用。 */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /** @return 当前用户 ID；匿名请求返回 {@code null} */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 获取必须存在的当前用户 ID。
     *
     * @return 当前用户 ID
     * @throws BusinessException 请求未经过有效登录链路时抛出
     */
    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    /** 清理线程变量，防止线程池复用造成身份串读。 */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
