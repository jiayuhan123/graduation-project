package xyz.jiayuhan.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthGlobalFilterTest {

    @Test
    void shouldReturnHttp401WhenTokenIsMissing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AuthGlobalFilter filter = new AuthGlobalFilter(objectMapper);
        filter.setWhitelist(List.of());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/user/me"));

        filter.filter(exchange, ignored -> {
            throw new AssertionError("unauthorized request must not reach downstream");
        }).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertEquals(401, body.get("code").asInt());
    }
}
