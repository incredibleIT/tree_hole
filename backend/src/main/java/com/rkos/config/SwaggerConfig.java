package com.rkos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI（Swagger）文档配置。
 * <p>
 * 定义 RKOS API 的元数据信息，供 springdoc-openapi 生成 Swagger UI 和 API 文档。
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI rkosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RKOS API")
                        .description("关系知识操作系统（RKOS）后端 API 文档")
                        .version("v1")
                        .contact(new Contact()
                                .name("RKOS 团队")
                                .url("https://github.com/rkos")));
    }
}
