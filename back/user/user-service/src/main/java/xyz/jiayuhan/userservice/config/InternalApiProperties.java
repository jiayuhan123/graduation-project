package xyz.jiayuhan.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 用户目录内部接口认证配置。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.internal-api")
public class InternalApiProperties {

    private boolean enabled;
    private String token;
}
