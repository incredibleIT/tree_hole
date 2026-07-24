package com.rkos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步基础设施配置。
 * <p>
 * 启用 Spring {@code @EnableAsync}，提供名为 {@code storyAgentExecutor} 的线程池，
 * 用于故事理解 Agent 异步处理。
 * <p>
 * 线程池参数：核心线程 5、最大线程 10、队列容量 50。
 * 拒绝策略：{@link ThreadPoolExecutor.CallerRunsPolicy}（队列满时降级为同步，保证请求不丢失）。
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Bean("storyAgentExecutor")
    public Executor storyAgentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 自定义 ThreadFactory 已内置 "story-agent-" 前缀和未捕获异常日志
        executor.setThreadFactory(loggingThreadFactory());
        executor.initialize();
        return executor;
    }

    /**
     * 创建带未捕获异常日志记录的线程工厂。
     * <p>
     * 当异步线程发生未捕获异常时，通过 ERROR 日志记录，
     * 避免异常静默丢失。
     */
    private ThreadFactory loggingThreadFactory() {
        AtomicInteger threadNumber = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, "story-agent-" + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setUncaughtExceptionHandler((thread, e) ->
                    log.error("异步线程未捕获异常: thread={}, error={}", thread.getName(), e.getMessage(), e));
            return t;
        };
    }
}
