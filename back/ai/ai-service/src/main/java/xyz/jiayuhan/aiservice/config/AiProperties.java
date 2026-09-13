package xyz.jiayuhan.aiservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Python Agent 网关、签名令牌与流式响应配置。 */
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private boolean enabled;
    private String agentBaseUrl = "http://localhost:8100";
    private String bridgeToken;
    private String toolSigningSecret;
    private Duration toolTokenTtl = Duration.ofMinutes(5);
    private Duration actionTokenTtl = Duration.ofMinutes(15);
    private Duration streamTimeout = Duration.ofSeconds(120);
    private Duration connectTimeout = Duration.ofSeconds(5);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAgentBaseUrl() {
        return agentBaseUrl;
    }

    public void setAgentBaseUrl(String agentBaseUrl) {
        this.agentBaseUrl = agentBaseUrl;
    }

    public String getBridgeToken() {
        return bridgeToken;
    }

    public void setBridgeToken(String bridgeToken) {
        this.bridgeToken = bridgeToken;
    }

    public String getToolSigningSecret() {
        return toolSigningSecret;
    }

    public void setToolSigningSecret(String toolSigningSecret) {
        this.toolSigningSecret = toolSigningSecret;
    }

    public Duration getToolTokenTtl() {
        return toolTokenTtl;
    }

    public void setToolTokenTtl(Duration toolTokenTtl) {
        this.toolTokenTtl = toolTokenTtl;
    }

    public Duration getActionTokenTtl() {
        return actionTokenTtl;
    }

    public void setActionTokenTtl(Duration actionTokenTtl) {
        this.actionTokenTtl = actionTokenTtl;
    }

    public Duration getStreamTimeout() {
        return streamTimeout;
    }

    public void setStreamTimeout(Duration streamTimeout) {
        this.streamTimeout = streamTimeout;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}
