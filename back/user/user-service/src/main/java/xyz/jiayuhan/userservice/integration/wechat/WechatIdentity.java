package xyz.jiayuhan.userservice.integration.wechat;

/** 微信开放平台下的可信用户身份；不同应用的 OpenID 不可混用。 */
public record WechatIdentity(String openId, String unionId) {
}
