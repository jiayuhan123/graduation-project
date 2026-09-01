package xyz.jiayuhan.userservice.integration.wechat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 微信小程序 code2session 配置；密钥只能由部署环境或配置中心注入。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.user.wechat")
public class WechatMiniProgramProperties {

    private boolean enabled;
    private String appId;
    private String secret;
    private String codeSessionUrl = "https://api.weixin.qq.com/sns/jscode2session";
}
