package com.rkos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 配置。
 * <p>
 * 启用 {@code @Retryable} 注解支持，底层依赖 AOP 代理拦截。
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
