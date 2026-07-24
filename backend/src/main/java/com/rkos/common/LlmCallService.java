package com.rkos.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * LLM 调用封装服务。
 * <p>
 * 所有 Agent 通过此服务统一调用大模型，自动处理重试（指数退避）和异常分类。
 * 底层使用 Spring AI 2.0.0 的 {@link ChatClient}，模型提供商通过 {@code application.yml} 配置切换。
 *
 * @see RkosException
 */
@Service
@Slf4j
public class LlmCallService {

    private final ChatClient chatClient;

    /**
     * 通过 {@link ChatClient.Builder}（Spring AI auto-config 自动提供）构建 ChatClient 实例。
     *
     * @param chatClientBuilder Spring AI 自动配置注入的 Builder
     */
    public LlmCallService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 调用 LLM 并返回文本响应。
     * <p>
     * 失败时自动重试最多 3 次（指数退避：1s → 2s → 4s）。
     * <ul>
     *   <li>超时 / 通用错误 → {@code LLM_CALL_FAILED}</li>
     *   <li>配额不足（HTTP 429） → {@code LLM_QUOTA_EXCEEDED}（不重试）</li>
     * </ul>
     *
     * @param prompt 发送给 LLM 的提示文本
     * @return LLM 返回的文本内容
     * @throws RkosException 当 LLM 调用失败时抛出
     */
    @Retryable(
            retryFor = {Exception.class},
            noRetryFor = {RkosException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String call(String prompt) {
        Objects.requireNonNull(prompt, "prompt 不能为 null");

        long startTime = System.currentTimeMillis();
        log.debug("LLM 调用开始, prompt 长度: {}", prompt.length());

        try {
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null
                    || response.getResult().getOutput() == null
                    || response.getResult().getOutput().getText() == null) {
                throw new RkosException("LLM_CALL_FAILED", "LLM 返回空响应");
            }

            long duration = System.currentTimeMillis() - startTime;
            String content = response.getResult().getOutput().getText();
            String modelUsed = response.getMetadata() != null ? response.getMetadata().getModel() : "unknown";

            log.info("LLM 调用成功, 耗时: {}ms, 模型: {}, 响应长度: {}", duration, modelUsed, content.length());
            return content;

        } catch (RkosException e) {
            // 已分类的业务异常（如配额不足），直接透传，Spring Retry 不会重试
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorCode = classifyException(e);

            if ("LLM_QUOTA_EXCEEDED".equals(errorCode)) {
                // 配额异常不重试，直接转为 RkosException
                log.error("LLM 配额不足, 耗时: {}ms, 异常: {}", duration, e.getMessage());
                throw new RkosException("LLM_QUOTA_EXCEEDED", "LLM 调用配额不足", e);
            }

            // 可重试的异常，保持原始类型让 Spring Retry 触发重试
            log.warn("LLM 调用异常（将重试）, 耗时: {}ms, 类型: {}, 消息: {}",
                    duration, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    /**
     * 根据异常特征分类为 RKOS 错误码。
     *
     * @param e 原始异常
     * @return 错误码字符串
     */
    private String classifyException(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            // HTTP 429 或明确的速率限制 / 配额不足
            if (lowerMessage.contains("429")
                    || lowerMessage.contains("quota")
                    || lowerMessage.contains("rate limit")
                    || lowerMessage.contains("too many requests")) {
                return "LLM_QUOTA_EXCEEDED";
            }
        }
        // 超时、连接异常、通用错误 → 可重试
        return "LLM_CALL_FAILED";
    }

    /**
     * 重试耗尽后的恢复方法。
     * <p>
     * Spring Retry 在 {@code @Retryable} 重试全部失败后调用此方法，
     * 将原始异常包装为 {@link RkosException}（LLM_CALL_FAILED），
     * 由 {@link GlobalExceptionHandler} 映射为 HTTP 503。
     *
     * @param e      最后一次重试失败的异常
     * @param prompt 原始 prompt 参数（用于日志）
     * @return 不会返回，始终抛出异常
     */
    @Recover
    public String recover(Exception e, String prompt) {
        log.error("LLM 调用重试耗尽, 异常: {}", e.getMessage());
        throw new RkosException("LLM_CALL_FAILED", "LLM 调用失败（重试耗尽）", e);
    }
}
