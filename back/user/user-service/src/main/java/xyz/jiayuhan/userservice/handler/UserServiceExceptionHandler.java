package xyz.jiayuhan.userservice.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;

/**
 * 用户服务安全与数据库约束异常转换器。
 *
 * <p>优先级高于公共异常处理器，用稳定业务码屏蔽框架异常和数据库约束细节。</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class UserServiceExceptionHandler {

    /** 将未登录、令牌过期等会话异常统一映射为 401 业务码。 */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException exception) {
        return response(ResultCode.UNAUTHORIZED);
    }

    /** 已登录但角色或权限不足时返回 403 业务码。 */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<Result<Void>> handleForbidden(SaTokenException exception) {
        return response(ResultCode.FORBIDDEN);
    }

    /** 兜底处理其他 Sa-Token 异常，避免向客户端暴露框架内部消息。 */
    @ExceptionHandler(SaTokenException.class)
    public ResponseEntity<Result<Void>> handleSaToken(SaTokenException exception) {
        return response(ResultCode.UNAUTHORIZED);
    }

    /** 将并发写入触发的唯一键、外键等约束冲突映射为稳定冲突响应。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrity(DataIntegrityViolationException exception) {
        return response(ResultCode.CONFLICT);
    }

    private ResponseEntity<Result<Void>> response(ResultCode resultCode) {
        return ResponseEntity.status(resultCode.getHttpStatus())
                .body(Result.error(resultCode));
    }
}
