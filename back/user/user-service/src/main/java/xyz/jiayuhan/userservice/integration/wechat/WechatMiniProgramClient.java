package xyz.jiayuhan.userservice.integration.wechat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import java.net.URI;

/** 微信小程序 code2session 客户端；session_key 不落库、不记录日志。 */
@Component
@RequiredArgsConstructor
public class WechatMiniProgramClient implements WechatIdentityClient {

    private final RestClient.Builder restClientBuilder;
    private final WechatMiniProgramProperties properties;

    @Override
    public String exchangeOpenId(String code) {
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
            WechatCodeSessionResponse response = restClientBuilder.build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(WechatCodeSessionResponse.class);
            if (response == null || !StringUtils.hasText(response.openid()) || response.errcode() != null) {
                throw new BusinessException(ResultCode.WECHAT_CODE_INVALID);
            }
            return response.openid();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信登录服务暂不可用");
        }
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()
                || !StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getSecret())) {
            throw new BusinessException(ResultCode.WECHAT_LOGIN_NOT_CONFIGURED);
        }
    }

    private record WechatCodeSessionResponse(String openid, Integer errcode, String errmsg) {
    }
}
