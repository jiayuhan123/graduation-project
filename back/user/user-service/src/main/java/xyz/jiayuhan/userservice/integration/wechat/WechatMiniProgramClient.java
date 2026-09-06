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

/** 微信小程序 code2session 客户端；session_key 不落库、不记录日志。 */
@Component
@RequiredArgsConstructor
public class WechatMiniProgramClient implements WechatIdentityClient {

    private final RestClient.Builder restClientBuilder;
    private final WechatMiniProgramProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public WechatIdentity exchangeIdentity(String code) {
        ensureConfigured();
        URI uri = UriComponentsBuilder.fromUriString(properties.getCodeSessionUrl())
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getSecret())
                .queryParam("js_code", code)
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
            WechatCodeSessionResponse response = parseResponse(responseBody);
            if (response == null || !StringUtils.hasText(response.openid()) || response.errcode() != null) {
                throw new BusinessException(ResultCode.WECHAT_CODE_INVALID);
            }
            return new WechatIdentity(response.openid(), response.unionid());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信登录服务暂不可用");
        }
    }

    private WechatCodeSessionResponse parseResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信登录服务返回空响应");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root == null || !root.isObject()) {
                throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信登录服务响应格式错误");
            }
            JsonNode errorCodeNode = root.get("errcode");
            Integer errorCode = errorCodeNode != null && errorCodeNode.canConvertToInt()
                    ? errorCodeNode.intValue()
                    : null;
            return new WechatCodeSessionResponse(
                    textValue(root, "openid"),
                    textValue(root, "unionid"),
                    errorCode,
                    textValue(root, "errmsg"));
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信登录服务响应格式错误");
        }
    }

    private String textValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node == null || !node.isString() ? null : node.asString();
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()
                || !StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getSecret())) {
            throw new BusinessException(ResultCode.WECHAT_LOGIN_NOT_CONFIGURED);
        }
    }

    private record WechatCodeSessionResponse(String openid, String unionid, Integer errcode, String errmsg) {
    }
}
