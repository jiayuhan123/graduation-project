package xyz.jiayuhan.common.web.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;

import java.util.stream.Collectors;

/**
 * 全局异常处理器（供所有 Servlet 业务服务复用，由 common-web 自动装配）。
 *
 * <p>所有异常统一转换为 {@link Result} 结构，同时保留正确的 HTTP 错误语义。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：保留业务码，并按其语义设置 HTTP 状态。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        log.warn("业务异常：code={}, message={}", exception.getCode(), exception.getMessage());
        return ResponseEntity.status(ResultCode.resolveHttpStatus(exception.getCode()))
                .body(Result.error(exception.getCode(), exception.getMessage()));
    }

    /** {@code @RequestBody} 参数校验失败（{@code @Valid}）。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", message);
        return response(ResultCode.BAD_REQUEST, message);
    }

    /** 单参数或表单参数校验失败（{@code @Validated}）。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", message);
        return response(ResultCode.BAD_REQUEST, message);
    }

    /** 文件超过服务配置的 multipart 上限时返回 413 和稳定的文件模块业务码。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception) {
        log.warn("上传文件大小超出限制：{}", exception.getMessage());
        return response(ResultCode.FILE_SIZE_EXCEEDED);
    }

    /** 保留控制器主动抛出的标准 HTTP 状态，例如 404、409 和 503。 */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<Result<Void>> handleErrorResponseException(ErrorResponseException exception) {
        return frameworkResponse(exception.getStatusCode());
    }

    /** 保留 MVC 产生的 404、405、415 等 Servlet 错误状态。 */
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<Result<Void>> handleServletException(ServletException exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            return frameworkResponse(errorResponse.getStatusCode());
        }
        return handleException(exception);
    }

    /** 未知异常兜底：记录完整堆栈，对外只暴露通用提示。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        log.error("系统异常", exception);
        return response(ResultCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Result<Void>> response(ResultCode resultCode) {
        return ResponseEntity.status(resultCode.getHttpStatus())
                .body(Result.error(resultCode));
    }

    private ResponseEntity<Result<Void>> response(ResultCode resultCode, String message) {
        return ResponseEntity.status(resultCode.getHttpStatus())
                .body(Result.error(resultCode.getCode(), message));
    }

    private ResponseEntity<Result<Void>> frameworkResponse(HttpStatusCode status) {
        String message = switch (status.value()) {
            case 400 -> "请求参数错误";
            case 401 -> "未登录，请先登录";
            case 403 -> "无权限访问";
            case 404 -> "请求路径不存在";
            case 405 -> "请求方法不支持";
            case 409 -> "数据冲突";
            case 413 -> "请求内容过大";
            case 415 -> "请求内容类型不支持";
            case 429 -> "请求过于频繁，请稍后再试";
            case 503 -> "服务暂不可用";
            default -> status.is5xxServerError() ? "服务器内部错误" : "请求失败";
        };
        return ResponseEntity.status(status).body(Result.error(status.value(), message));
    }
}
