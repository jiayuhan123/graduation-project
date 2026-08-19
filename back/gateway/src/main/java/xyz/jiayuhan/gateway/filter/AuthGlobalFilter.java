package xyz.jiayuhan.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
import reactor.core.scheduler.Schedulers;
import xyz.jiayuhan.common.core.result.Result;

import java.util.List;

/**
 * 网关全局会话校验过滤器。
 *
 * <p>除白名单外的请求必须携带有效 Sa-Token。过滤器会先移除客户端伪造的
 * {@code X-User-ID}，校验成功后再用服务端解析出的用户 ID 重建该请求头供下游使用。
 * 具体角色与权限仍由各业务服务在接口层校验。</p>
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "security")
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private List<String> whitelist;

    /** 绑定配置文件中的匿名访问路径模式。 */
    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 客户端传入的身份头一律删除，只有鉴权成功后才由网关重建。
        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> headers.remove("X-User-ID")))
                .build();

        // 白名单只跳过会话校验，仍使用已经删除不可信身份头的请求继续转发。
        if (isWhitelisted(path)) {
            return chain.filter(sanitizedExchange);
        }

        // 令牌名称与用户服务保持一致，由 Sa-Token 配置统一决定。
        String token = sanitizedExchange.getRequest().getHeaders().getFirst(StpUtil.getTokenName());
        if (!StringUtils.hasText(token)) {
            return writeUnauthorized(exchange, "请先登录");
        }

        // Sa-Token 会访问 Redis；切换到有界弹性线程池，避免阻塞 Gateway 的事件循环。
        return Mono.fromCallable(() -> {
            return StpUtil.getLoginIdByToken(token);
        }).flatMap(userId -> {
            if (userId == null) {
                return writeUnauthorized(exchange, "登录已过期，请重新登录");
            }
            // 只注入由服务端令牌解析得到的身份，供下游进行审计或数据范围过滤。
            ServerWebExchange mutated = sanitizedExchange.mutate()
                    .request(r -> r.headers(headers -> headers.set("X-User-ID", String.valueOf(userId))))
                    .build();
            return chain.filter(mutated);
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> {
                    log.debug("Token 校验失败：{}", error.getMessage());
                    return writeUnauthorized(sanitizedExchange, "登录已过期，请重新登录");
                });
    }

    /** 判断路径是否匹配任一匿名访问模式。 */
    private boolean isWhitelisted(String path) {
        if (whitelist == null) {
            return false;
        }
        for (String pattern : whitelist) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 写出项目统一格式的未授权响应。
     *
     * <p>HTTP 状态沿用项目现有的 200 约定，真实错误语义由响应体业务码 401 表达。</p>
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
        // 在常规路由过滤器之前完成身份头清理和会话校验。
        return -100;
    }
}
