package xyz.jiayuhan.common.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务错误码枚举
 * <p>
 * 错误码规范：
 * - 200：操作成功
 * - 4xx：客户端错误（参数、权限、资源等）
 * - 5xx：服务器端错误
 * - 1xxx：用户模块业务错误
 * - 2xxx：文件模块业务错误
 * </p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // =================== 通用成功 ===================
    SUCCESS(200, "操作成功"),

    // =================== 通用错误 ===================
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // =================== 用户账号（1xxx） ===================
    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    PHONE_EXISTS(1003, "手机号已被注册"),
    USER_DISABLED(1004, "账号已被禁用，请联系管理员"),
    OLD_PASSWORD_ERROR(1005, "旧密码不正确"),
    USER_NOT_FOUND(1006, "用户不存在"),
    EMAIL_EXISTS(1007, "邮箱已被注册"),
    LOGIN_TOO_FREQUENT(1008, "登录失败次数过多，请稍后再试"),
    ROLE_NOT_FOUND(1009, "角色不存在"),
    ROLE_CODE_EXISTS(1010, "角色编码已存在"),
    BUILT_IN_ROLE_PROTECTED(1011, "内置角色不允许删除或修改编码"),
    CANNOT_OPERATE_SELF(1012, "不能对当前登录账号执行此操作"),
    ROLE_IN_USE(1013, "角色正在使用，不能删除"),
    PERMISSION_NOT_FOUND(1014, "权限不存在"),
    NEW_PASSWORD_SAME_AS_OLD(1015, "新密码不能与旧密码相同"),

    // =================== 文件模块（2xxx） ===================
    FILE_NOT_FOUND(2000, "文件不存在"),
    FILE_UPLOAD_FAILED(2001, "文件上传失败"),
    FILE_SIZE_EXCEEDED(2002, "文件大小超出限制"),
    FILE_TYPE_NOT_ALLOWED(2003, "文件类型不允许"),

    // =================== 通知模块（3xxx） ===================
    NOTIFICATION_NOT_FOUND(3000, "通知不存在"),

    // =================== 审计模块（4xxx） ===================
    AUDIT_LOG_NOT_FOUND(4000, "审计记录不存在"),
    ;

    /**
     * 业务状态码
     */
    private final Integer code;

    /**
     * 描述信息
     */
    private final String message;
}
