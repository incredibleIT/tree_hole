package com.rkos.modules.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rkos.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API Key 认证拦截器。
 * <p>
 * 在请求到达 Controller 之前校验 {@code X-API-Key} 请求头。
 * 验证失败时直接写入 401 JSON 响应（不经过 GlobalExceptionHandler）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyAuthService apiKeyAuthService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (!apiKeyAuthService.validate(apiKey)) {
            log.warn("API Key 认证失败: 路径={}, 方法={}, 来源={}",
                    request.getRequestURI(), request.getMethod(), request.getRemoteAddr());

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "UNAUTHORIZED", "无效的 API Key", null);
            objectMapper.writeValue(response.getWriter(), errorResponse);
            return false;
        }

        return true;
    }
}
