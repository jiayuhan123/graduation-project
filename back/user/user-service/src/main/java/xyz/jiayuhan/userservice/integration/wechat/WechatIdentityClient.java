package xyz.jiayuhan.userservice.integration.wechat;

/** 把微信一次性登录 code 换成可信 OpenID。 */
public interface WechatIdentityClient {

    String exchangeOpenId(String code);
}
