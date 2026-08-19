package xyz.jiayuhan.userservice.event;

/** 用户服务发布到审计服务的稳定操作编码。 */
public final class UserAuditActions {

    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED";
    public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
    public static final String USER_ROLES_CHANGED = "USER_ROLES_CHANGED";
    public static final String USER_PASSWORD_RESET = "USER_PASSWORD_RESET";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String SELF_PROFILE_UPDATED = "SELF_PROFILE_UPDATED";
    public static final String SELF_PASSWORD_CHANGED = "SELF_PASSWORD_CHANGED";
    public static final String ROLE_CREATED = "ROLE_CREATED";
    public static final String ROLE_UPDATED = "ROLE_UPDATED";
    public static final String ROLE_STATUS_CHANGED = "ROLE_STATUS_CHANGED";
    public static final String ROLE_PERMISSIONS_CHANGED = "ROLE_PERMISSIONS_CHANGED";
    public static final String ROLE_DELETED = "ROLE_DELETED";

    private UserAuditActions() {
    }
}
