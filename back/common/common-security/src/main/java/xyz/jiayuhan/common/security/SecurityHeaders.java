package xyz.jiayuhan.common.security;

/** 网关与 Servlet 业务服务之间约定的可信身份请求头。 */
public final class SecurityHeaders {

    /** 网关校验令牌后写入的用户 ID 请求头。 */
    public static final String USER_ID = "X-User-ID";

    /** 服务间 HTTP 调用使用的共享认证请求头。 */
    public static final String INTERNAL_TOKEN = "X-Internal-Token";

    private SecurityHeaders() {
    }
}
