package xyz.jiayuhan.userservice.model.response.auth;

/** 网页微信扫码授权入口；state 同时写入 HttpOnly Cookie，前端只需跳转 authorizationUrl。 */
public record WechatWebAuthorizeResponse(
        String authorizationUrl,
        String state,
        long expiresInSeconds
) {
}
