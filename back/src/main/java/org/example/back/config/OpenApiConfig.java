package org.example.back.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// 这是一个配置类，用于设置 OpenAPI（Swagger）的相关信息和安全方案
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI warehouseOpenApi() {
        String schemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("仓库管理系统后端 API")
                        .description("全项目版：覆盖认证授权、系统管理、安全治理、基础资料、进销退存业务与销售统计分析，支持作废与红冲闭环流程")
                        .version("v1.6.0")
                        .contact(new Contact().name("WMS Backend Team")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
