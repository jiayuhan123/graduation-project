package xyz.jiayuhan.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.jiayuhan.common.result.Result;

/**
 * 网关全局异常处理器
 */
@Slf4j
@Order(-1) // 优先级比默认的低，越小优先级越高，Spring 默认是 -1，为了覆盖它设为更小或者直接 -1 被我们注入替换
public class GlobalExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 设置返回格式为 JSON
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 默认状态码 500
        Result<Void> result = Result.error(500, "网关内部错误：" + ex.getMessage());

        // 判断异常类型
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            HttpStatus status = HttpStatus.resolve(responseStatusException.getStatusCode().value());
            if (status != null) {
                if (status == HttpStatus.NOT_FOUND) {
                    result = Result.error(404, "请求路径不存在");
                } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
                    result = Result.error(503, "服务不可用");
                } else if (status == HttpStatus.UNAUTHORIZED) {
                    result = Result.error(401, "未授权的访问");
                } else {
                    result = Result.error(status.value(), status.getReasonPhrase());
                }
            }
        }

        // 可以设置 HTTP 状态码统一为 200，错误信息在 JSON 中体现
        response.setStatusCode(HttpStatus.OK);
        
        final Result<Void> finalResult = result;

        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(finalResult);
                return bufferFactory.wrap(bytes);
            } catch (JsonProcessingException e) {
                log.error("Error writing response", e);
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }
}
