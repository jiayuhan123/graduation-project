package xyz.jiayuhan.common.openapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 各业务服务可覆盖的 OpenAPI 文档元数据。 */
@ConfigurationProperties(prefix = "app.openapi")
public class OpenApiProperties {

    private String title = "微服务 API";
    private String description = "微服务接口文档";
    private String version = "1.0.0";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
