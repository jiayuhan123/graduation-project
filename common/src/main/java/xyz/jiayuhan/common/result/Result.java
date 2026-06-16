package xyz.jiayuhan.common.result;

import lombok.Data;

/**
 * 全局统一返回结果类
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
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    public Result() {
    }

    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
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
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    /**
     * 失败响应（自定义错误码和错误信息）
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    /**
     * 失败响应（自定义错误信息，默认 500 错误码）
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
}
