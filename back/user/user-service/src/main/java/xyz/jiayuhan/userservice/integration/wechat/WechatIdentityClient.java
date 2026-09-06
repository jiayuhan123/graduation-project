package xyz.jiayuhan.userservice.integration.wechat;

/** 把微信小程序一次性登录 code 换成可信身份。 */
public interface WechatIdentityClient {

    WechatIdentity exchangeIdentity(String code);
}
