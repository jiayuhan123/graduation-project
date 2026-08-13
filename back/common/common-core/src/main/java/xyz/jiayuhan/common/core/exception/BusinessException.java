package xyz.jiayuhan.common.core.exception;

import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * 用于在业务逻辑层抛出预期内的业务错误（如用户名已存在、密码错误等）。
 * 与 RuntimeException 区别：BusinessException 是"已知"的业务异常，
 * RuntimeException 是"未知"的系统异常。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 *     if (userExists) {
 *         throw new BusinessException(ResultCode.USERNAME_EXISTS);
 *     }
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 业务错误码
     */
    private final Integer code;

    /**
     * 通过 ResultCode 枚举构造
     *
     * @param resultCode 错误码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 通过 ResultCode 枚举 + 自定义消息构造
     *
     * @param resultCode 错误码枚举（使用其 code）
     * @param message    自定义错误描述
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 通过自定义错误码和消息构造
     *
     * @param code    错误码
     * @param message 错误描述
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
