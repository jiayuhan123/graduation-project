package xyz.jiayuhan.common.openapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** 为 Servlet 业务服务统一声明 OpenAPI 元数据和 Sa-Token 请求头认证方案。 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@EnableConfigurationProperties(OpenApiProperties.class)
public class CommonOpenApiAutoConfiguration {

    private static final String TOKEN_SCHEME = "SaToken";

    /** 根据服务自身配置创建 OpenAPI 文档模型。 */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI serviceOpenApi(OpenApiProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.getTitle())
                        .description(properties.getDescription())
                        .version(properties.getVersion()))
                .addSecurityItem(new SecurityRequirement().addList(TOKEN_SCHEME))
                .components(new Components().addSecuritySchemes(TOKEN_SCHEME,
                        new SecurityScheme()
                                .name("satoken")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)));
    }
}
