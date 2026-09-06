package xyz.jiayuhan.userservice.integration.wechat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import java.net.URI;

/** 微信开放平台网站应用 OAuth 客户端。访问令牌只用于本次身份交换，不落库、不记录日志。 */
@Component
@RequiredArgsConstructor
public class WechatWebClient {

    private final RestClient.Builder restClientBuilder;
    private final WechatWebProperties properties;
    private final ObjectMapper objectMapper;

    public String authorizationUrl(String state) {
        ensureConfigured();
        return UriComponentsBuilder.fromUriString(properties.getAuthorizeUrl())
                .queryParam("appid", properties.getAppId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "snsapi_login")
                .queryParam("state", state)
                .fragment("wechat_redirect")
                .build()
                .encode()
                .toUriString();
    }

    public WechatIdentity exchangeIdentity(String code) {
        ensureConfigured();
        URI uri = UriComponentsBuilder.fromUriString(properties.getAccessTokenUrl())
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getSecret())
                .queryParam("code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .encode()
                .toUri();
        try {
            String responseBody = restClientBuilder.build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            return parseIdentity(responseBody);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信扫码登录服务暂不可用");
        }
    }

    private WechatIdentity parseIdentity(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信扫码登录服务返回空响应");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String openId = textValue(root, "openid");
            JsonNode errorCode = root == null ? null : root.get("errcode");
            if (root == null || !root.isObject() || errorCode != null || !StringUtils.hasText(openId)) {
                throw new BusinessException(ResultCode.WECHAT_CODE_INVALID);
            }
            return new WechatIdentity(openId, textValue(root, "unionid"));
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信扫码登录服务响应格式错误");
        }
    }

    private String textValue(JsonNode root, String fieldName) {
        if (root == null) {
            return null;
        }
        JsonNode node = root.get(fieldName);
        return node == null || !node.isString() ? null : node.asString();
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()
                || !StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getSecret())
                || !StringUtils.hasText(properties.getRedirectUri())) {
            throw new BusinessException(ResultCode.WECHAT_LOGIN_NOT_CONFIGURED);
        }
    }
}
