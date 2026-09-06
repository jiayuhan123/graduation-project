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
    SUCCESS(200, "操作成功", 200),

    // =================== 通用错误 ===================
    BAD_REQUEST(400, "请求参数错误", 400),
    UNAUTHORIZED(401, "未登录，请先登录", 401),
    FORBIDDEN(403, "无权限访问", 403),
    NOT_FOUND(404, "资源不存在", 404),
    CONFLICT(409, "数据冲突", 409),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试", 429),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误", 500),
    SERVICE_UNAVAILABLE(503, "服务暂不可用", 503),

    // =================== 用户账号（1xxx） ===================
    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误", 401),
    USERNAME_EXISTS(1002, "用户名已存在", 409),
    PHONE_EXISTS(1003, "手机号已被注册", 409),
    USER_DISABLED(1004, "账号已被禁用，请联系管理员", 403),
    OLD_PASSWORD_ERROR(1005, "旧密码不正确", 400),
    USER_NOT_FOUND(1006, "用户不存在", 404),
    EMAIL_EXISTS(1007, "邮箱已被注册", 409),
    LOGIN_TOO_FREQUENT(1008, "登录失败次数过多，请稍后再试", 429),
    ROLE_NOT_FOUND(1009, "角色不存在", 404),
    ROLE_CODE_EXISTS(1010, "角色编码已存在", 409),
    BUILT_IN_ROLE_PROTECTED(1011, "内置角色不允许删除或修改编码", 409),
    CANNOT_OPERATE_SELF(1012, "不能对当前登录账号执行此操作", 409),
    ROLE_IN_USE(1013, "角色正在使用，不能删除", 409),
    PERMISSION_NOT_FOUND(1014, "权限不存在", 404),
    NEW_PASSWORD_SAME_AS_OLD(1015, "新密码不能与旧密码相同", 400),
    WECHAT_LOGIN_NOT_CONFIGURED(1016, "微信登录尚未配置", 503),
    WECHAT_CODE_INVALID(1017, "微信登录凭证无效或已过期", 401),
    WECHAT_ALREADY_BOUND(1018, "该微信账号已绑定其他用户", 409),
    LOGIN_CREDENTIAL_REQUIRED(1019, "账号必须至少保留一种登录方式", 409),
    STUDENT_PROFILE_REQUIRED(1020, "学生角色必须配置学生资料", 409),
    TEACHER_PROFILE_REQUIRED(1021, "教师角色必须配置教师资料", 409),
    STUDENT_NO_EXISTS(1023, "学号已存在", 409),
    TEACHER_NO_EXISTS(1024, "教师工号已存在", 409),
    PASSWORD_NOT_SET(1025, "当前账号尚未设置密码", 409),
    PASSWORD_ALREADY_SET(1026, "当前账号已经设置密码", 409),

    // =================== 文件模块（2xxx） ===================
    FILE_NOT_FOUND(2000, "文件不存在", 404),
    FILE_UPLOAD_FAILED(2001, "文件上传失败", 500),
    FILE_SIZE_EXCEEDED(2002, "文件大小超出限制", 413),
    FILE_TYPE_NOT_ALLOWED(2003, "文件类型不允许", 415),

    // =================== 通知模块（3xxx） ===================
    NOTIFICATION_NOT_FOUND(3000, "通知不存在", 404),

    // =================== 审计模块（4xxx） ===================
    AUDIT_LOG_NOT_FOUND(4000, "审计记录不存在", 404),

    // =================== 内容题库（5xxx） ===================
    CONTENT_MODULE_NOT_FOUND(5000, "内容模块不存在", 404),
    KNOWLEDGE_POINT_NOT_FOUND(5001, "知识点不存在", 404),
    QUESTION_NOT_FOUND(5002, "题目不存在", 404),
    ERROR_TYPE_NOT_FOUND(5003, "错因类型不存在", 404),
    CONTENT_CODE_EXISTS(5004, "内容编码已存在", 409),
    CONTENT_IN_USE(5005, "内容正在被引用，不能执行当前操作", 409),
    CONTENT_VERSION_CONFLICT(5006, "内容已被其他人修改，请刷新后重试", 409),
    CONTENT_RELATION_INVALID(5007, "内容关联关系不正确", 409),
    QUESTION_STATE_INVALID(5008, "题目当前状态不允许执行该操作", 409),

    // =================== 学习闭环（6xxx） ===================
    PRACTICE_TASK_NOT_FOUND(6000, "练习任务不存在", 404),
    PHOTO_MARKING_TASK_NOT_FOUND(6001, "拍照批改任务不存在", 404),

    // =================== 教学（7xxx） ===================
    TEACHING_CLASS_NOT_FOUND(7000, "班级不存在", 404),
    ASSIGNMENT_NOT_FOUND(7001, "作业不存在", 404),
    TEACHING_CLASS_CODE_EXISTS(7002, "班级码已存在", 409),
    TEACHING_CLASS_INACTIVE(7003, "班级已归档，不能执行当前操作", 409),
    TEACHING_MEMBERSHIP_NOT_FOUND(7004, "班级成员关系不存在", 404),
    TEACHING_ALREADY_JOINED(7005, "已经加入该班级", 409),
    TEACHING_VERSION_CONFLICT(7006, "教学数据已被其他人修改，请刷新后重试", 409),
    ASSIGNMENT_STATE_INVALID(7007, "作业当前状态不允许执行该操作", 409),
    ASSIGNMENT_CONTENT_INVALID(7008, "作业题目或分层配置不正确", 409),
    ASSIGNMENT_SUBMISSION_NOT_FOUND(7009, "作业提交记录不存在", 404),

    // =================== 游戏（8xxx） ===================
    GAME_LEVEL_NOT_FOUND(8000, "游戏关卡不存在", 404),
    GAME_SESSION_NOT_FOUND(8001, "游戏对局不存在", 404),
    ;

    /**
     * 业务状态码
     */
    private final Integer code;

    /**
     * 描述信息
     */
    private final String message;

    /**
     * 对应的 HTTP 状态码。业务码用于客户端精确判断，HTTP 状态用于网关、客户端和监控识别错误类型。
     */
    private final Integer httpStatus;

    /** 将业务码解析为 HTTP 状态；未登记的自定义业务异常按客户端请求错误处理。 */
    public static int resolveHttpStatus(Integer code) {
        if (code == null) {
            return INTERNAL_SERVER_ERROR.httpStatus;
        }
        for (ResultCode resultCode : values()) {
            if (resultCode.code.equals(code)) {
                return resultCode.httpStatus;
            }
        }
        return code >= 400 && code <= 599 ? code : BAD_REQUEST.httpStatus;
    }
}
