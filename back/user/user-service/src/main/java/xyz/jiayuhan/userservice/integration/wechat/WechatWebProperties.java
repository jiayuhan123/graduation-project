package xyz.jiayuhan.userservice.integration.wechat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 微信开放平台网站应用配置；AppSecret 只能由部署环境或配置中心注入。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.user.wechat.web")
public class WechatWebProperties {

    private boolean enabled;
    private String appId;
    private String secret;
    private String redirectUri;
    private String authorizeUrl = "https://open.weixin.qq.com/connect/qrconnect";
    private String accessTokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private Duration stateTtl = Duration.ofMinutes(10);
}
