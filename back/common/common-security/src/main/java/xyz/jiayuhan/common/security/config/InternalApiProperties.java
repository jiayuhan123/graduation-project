package xyz.jiayuhan.common.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 所有业务服务共用的内部 HTTP 接口认证配置。 */
@ConfigurationProperties(prefix = "app.internal-api")
public class InternalApiProperties {

    private boolean enabled;
    private String token;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
