package xyz.jiayuhan.gateway.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(objectMapper);

    @Test
    void shouldPreserveResponseStatusExceptionStatus() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/missing"));

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND)).block();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertEquals(404, body.get("code").asInt());
        assertEquals("请求路径不存在", body.get("message").asText());
    }

    @Test
    void shouldReturnSafeServerErrorWithoutLeakingExceptionDetail() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/broken"));

        handler.handle(exchange, new IllegalStateException("database-password")) .block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        String bodyText = exchange.getResponse().getBodyAsString().block();
        assertFalse(bodyText.contains("database-password"));
        JsonNode body = objectMapper.readTree(bodyText);
        assertEquals(500, body.get("code").asInt());
        assertEquals("网关内部错误", body.get("message").asText());
    }
}
