package xyz.jiayuhan.userservice.model.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 微信开放平台网站应用授权回调参数。 */
public record WechatWebCallbackRequest(
        @NotBlank(message = "微信授权 code 不能为空") @Size(max = 256) String code,
        @NotBlank(message = "微信授权 state 不能为空") @Size(max = 128) String state
) {
}
