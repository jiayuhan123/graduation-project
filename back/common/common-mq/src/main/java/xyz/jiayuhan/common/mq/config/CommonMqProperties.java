package xyz.jiayuhan.common.mq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** MQ 公共发布参数。 */
@ConfigurationProperties(prefix = "app.mq")
public class CommonMqProperties {

    /** 等待 Broker publisher confirm 的最大时长。 */
    private Duration confirmTimeout = Duration.ofSeconds(5);

    public Duration getConfirmTimeout() {
        return confirmTimeout;
    }

    public void setConfirmTimeout(Duration confirmTimeout) {
        this.confirmTimeout = confirmTimeout;
    }
}
