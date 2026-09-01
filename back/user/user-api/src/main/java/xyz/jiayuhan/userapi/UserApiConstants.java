package xyz.jiayuhan.userapi;

/** 用户服务内部 HTTP 契约使用的稳定常量。 */
public final class UserApiConstants {

    public static final String SERVICE_NAME = "user-service";
    public static final String INTERNAL_USERS_PATH = "/internal/users";
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private UserApiConstants() {
    }
}
