package xyz.jiayuhan.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 用户服务 OpenAPI 文档配置。 */
@Configuration
public class OpenApiConfig {

    private static final String TOKEN_SCHEME = "SaToken";

    /**
     * 声明用户服务元数据及 Sa-Token 请求头认证方案，使 Swagger UI 可直接调试受保护接口。
     */
    @Bean
    public OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("通用用户服务 API")
                        .description("认证、个人账户、用户管理与 RBAC 接口")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(TOKEN_SCHEME))
                .components(new Components().addSecuritySchemes(TOKEN_SCHEME,
                        new SecurityScheme()
                                .name("satoken")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)));
    }
}
