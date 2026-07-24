package com.rkos.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rkos.modules.auth.service.ApiKeyAuthInterceptor;
import com.rkos.modules.auth.service.ApiKeyAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link ApiKeyAuthInterceptor} 单元测试。
 * <p>
 * 直接调用 {@code preHandle} 方法测试拦截器逻辑，
 * 使用 Mock 对象验证认证行为和 401 响应格式。
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyAuthInterceptorTest {

    private ApiKeyAuthInterceptor interceptor;

    @Mock
    private ApiKeyAuthService apiKeyAuthService;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        interceptor = new ApiKeyAuthInterceptor(apiKeyAuthService, objectMapper);
        response = new MockHttpServletResponse();
    }

    @Test
    void validApiKey_shouldReturnTrueAndPassThrough() throws Exception {
        when(apiKeyAuthService.validate("valid-key-123")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stories");
        request.addHeader("X-API-Key", "valid-key-123");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result, "有效 Key 应放行");
    }

    @Test
    void missingApiKeyHeader_shouldReturn401WithCorrectJson() throws Exception {
        when(apiKeyAuthService.validate(null)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stories");
        // 不添加 X-API-Key Header

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result, "缺少 Key 应拦截");
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"), "Content-Type 应包含 application/json");

        String body = response.getContentAsString();
        assertTrue(body.contains("\"code\":\"UNAUTHORIZED\""), "响应应包含 UNAUTHORIZED code");
        assertTrue(body.contains("\"message\":\"无效的 API Key\""), "响应应包含错误消息");
        assertTrue(body.contains("\"data\":null"), "响应 data 应为 null");
        assertTrue(body.contains("\"timestamp\""), "响应应包含 timestamp");
    }

    @Test
    void invalidApiKey_shouldReturn401() throws Exception {
        when(apiKeyAuthService.validate("wrong-key")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stories");
        request.addHeader("X-API-Key", "wrong-key");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result, "无效 Key 应拦截");
        assertEquals(401, response.getStatus());
    }

    @Test
    void expiredApiKey_shouldReturn401() throws Exception {
        when(apiKeyAuthService.validate("expired-key")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stories");
        request.addHeader("X-API-Key", "expired-key");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result, "过期 Key 应拦截");
        assertEquals(401, response.getStatus());
    }

    @Test
    void disabledApiKey_shouldReturn401() throws Exception {
        when(apiKeyAuthService.validate("disabled-key")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stories");
        request.addHeader("X-API-Key", "disabled-key");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result, "禁用 Key 应拦截");
        assertEquals(401, response.getStatus());
    }

    @Test
    void responseContentTypeAndEncoding_shouldBeCorrect() throws Exception {
        when(apiKeyAuthService.validate("bad")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stories");
        request.addHeader("X-API-Key", "bad");

        interceptor.preHandle(request, response, new Object());

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"), "Content-Type 应包含 application/json");
        assertEquals("UTF-8", response.getCharacterEncoding());
    }
}
