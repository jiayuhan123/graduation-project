package xyz.jiayuhan.userservice.model.request.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 当前账号绑定微信时使用的一次性登录凭证。 */
public record WechatCodeRequest(
        @NotBlank(message = "微信登录凭证不能为空")
        @Size(max = 256, message = "微信登录凭证过长")
        String code
) {
}
