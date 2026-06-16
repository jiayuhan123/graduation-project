package xyz.jiayuhan.gateway.filter;

import cn.dev33.satoken.reactor.context.SaReactorHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.jiayuhan.common.result.Result;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关全局鉴权过滤器
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "security")
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private List<String> whitelist;

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 1. 白名单检查
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取 Token
        String token = exchange.getRequest().getHeaders().getFirst("satoken");
        if (!StringUtils.hasText(token)) {
            return writeUnauthorized(exchange, "请先登录");
        }

        // 3. 验证 Token（查 Redis）
        return Mono.fromCallable(() -> {
            // 使用 Sa-Token API 获取 userId (根据 token 直接查底层 Redis)
            return cn.dev33.satoken.stp.StpUtil.getLoginIdByToken(token);
        }).flatMap(userId -> {
            if (userId == null) {
                return writeUnauthorized(exchange, "登录已过期，请重新登录");
            }
            // 4. 注入 X-User-ID 头给下游微服务
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.header("X-User-ID", String.valueOf(userId)))
                    .build();
            return chain.filter(mutated);
        });
    }

    /**
     * 判断路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        for (String pattern : whitelist) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回 401 统一未授权响应
     */
    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = Result.error(401, msg);

        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(result);
                return bufferFactory.wrap(bytes);
            } catch (JsonProcessingException e) {
                log.error("JSON 序列化异常", e);
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
