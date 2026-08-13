package xyz.jiayuhan.common.web.context;

/**
 * 当前线程用户上下文持有者
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前线程的用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID_THREAD_LOCAL.set(userId);
    }

    /**
     * 获取当前线程的用户 ID
     */
    public static Long getUserId() {
        return USER_ID_THREAD_LOCAL.get();
    }

    /**
     * 清除当前线程的用户 ID，防止内存泄漏
     */
    public static void removeUserId() {
        USER_ID_THREAD_LOCAL.remove();
    }
}
