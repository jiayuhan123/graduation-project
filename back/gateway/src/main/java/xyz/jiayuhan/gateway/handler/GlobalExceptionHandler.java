package xyz.jiayuhan.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import xyz.jiayuhan.common.core.result.Result;

/** 网关全局异常处理器：统一响应体，并保留真实的 HTTP 状态。 */
@Slf4j
@Order(-1)
public class GlobalExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(exception);
        }

        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "网关内部错误";

        if (exception instanceof ResponseStatusException responseStatusException) {
            status = responseStatusException.getStatusCode();
            message = messageFor(status);
            log.warn("网关请求失败：status={}, path={}", status.value(), exchange.getRequest().getPath());
        } else {
            log.error("网关内部异常：path={}", exchange.getRequest().getPath(), exception);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<Void> result = Result.error(status.value(), message);

        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(result));
            } catch (JsonProcessingException serializationException) {
                log.error("网关错误响应序列化失败", serializationException);
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }

    private String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "请求参数错误";
            case 401 -> "未授权的访问";
            case 403 -> "无权限访问";
            case 404 -> "请求路径不存在";
            case 405 -> "请求方法不支持";
            case 429 -> "请求过于频繁";
            case 503 -> "服务不可用";
            default -> status.is5xxServerError() ? "网关内部错误" : "请求失败";
        };
    }
}
