package com.rkos.config;

import com.rkos.modules.auth.service.ApiKeyAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API Key 认证拦截器配置。
 * <p>
 * 注册 {@link ApiKeyAuthInterceptor} 拦截所有 {@code /api/v1/**} 请求，
 * 并排除健康检查、Swagger UI、API 文档等公开端点。
 */
@Configuration
@ConditionalOnProperty(name = "rkos.api.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ApiKeyAuthConfig implements WebMvcConfigurer {

    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAuthInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/health",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}
