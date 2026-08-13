package xyz.jiayuhan.common.core.result;

import lombok.Data;
import xyz.jiayuhan.common.core.exception.ResultCode;

/**
 * 全局统一返回结果类
 *
 * @param <T> 数据泛型
 */
@Data
public class Result<T> {

    /**
     * 业务状态码 (例如 200 表示成功，500 表示系统内部错误，401 表示未授权)
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public Result() {
    }

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功响应（自定义提示信息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应（错误码枚举）
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 失败响应（自定义错误码和错误信息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应（自定义错误信息，默认 500 错误码）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
