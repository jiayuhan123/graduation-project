package xyz.jiayuhan.userservice.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 后端统一返回结果集 (泛型类)
 * 规范前后端交互的数据格式，不论接口成功还是失败，都会返回这个结构的 JSON。
 * @param <T> 具体返回的数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码
     * 200: 成功
     * 500: 服务器内部错误
     * 401: 未登录或 Token 失效
     * 403: 无权限访问
     */
    private Integer code;

    /**
     * 提示信息
     * 例如："登录成功"、"账号密码错误"
     */
    private String msg;

    /**
     * 真正返回给前端的业务数据
     * 例如：用户信息对象、商品列表数组等
     */
    private T data;

    /**
     * 私有构造方法，强制使用静态工厂方法创建对象
     */
    private Result() {}

    /**
     * 内部全参构造
     */
    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ============================ 成功返回 ============================

    /**
     * 成功返回，没有具体数据 (例如：删除操作)
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 成功返回，带有具体业务数据 (例如：查询操作)
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功返回，自定义提示信息和数据
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    // ============================ 失败返回 ============================

    /**
     * 失败返回，仅带错误提示 (默认抛出 500 服务器错误)
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    /**
     * 失败返回，自定义状态码和错误提示 (供全局异常处理器使用)
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}
