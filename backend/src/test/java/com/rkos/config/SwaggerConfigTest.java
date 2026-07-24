package com.rkos.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SwaggerConfig} 单元测试。
 * <p>
 * 直接实例化配置类验证 OpenAPI Bean 元数据正确性，
 * 不加载 Spring 上下文。
 */
class SwaggerConfigTest {

    private final SwaggerConfig swaggerConfig = new SwaggerConfig();

    @Test
    void rkosOpenAPI_shouldReturnOpenAPIWithCorrectTitle() {
        OpenAPI openAPI = swaggerConfig.rkosOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("RKOS API", openAPI.getInfo().getTitle());
    }

    @Test
    void rkosOpenAPI_shouldReturnOpenAPIWithVersionV1() {
        OpenAPI openAPI = swaggerConfig.rkosOpenAPI();

        assertEquals("v1", openAPI.getInfo().getVersion());
    }

    @Test
    void rkosOpenAPI_shouldReturnOpenAPIWithDescription() {
        OpenAPI openAPI = swaggerConfig.rkosOpenAPI();

        assertNotNull(openAPI.getInfo().getDescription());
        assertTrue(openAPI.getInfo().getDescription().contains("RKOS"));
    }

    @Test
    void rkosOpenAPI_shouldReturnOpenAPIWithContactInfo() {
        OpenAPI openAPI = swaggerConfig.rkosOpenAPI();

        assertNotNull(openAPI.getInfo().getContact());
        assertEquals("RKOS 团队", openAPI.getInfo().getContact().getName());
        assertEquals("https://github.com/rkos", openAPI.getInfo().getContact().getUrl());
    }
}
