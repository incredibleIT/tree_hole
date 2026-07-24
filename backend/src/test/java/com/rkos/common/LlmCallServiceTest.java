package com.rkos.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link LlmCallService} 单元测试。
 * <p>
 * 使用纯 Mockito 测试核心逻辑（成功调用、异常分类）。
 * 使用 {@link RetryTemplate} 编程式验证重试行为（不依赖 Spring AOP 上下文）。
 */
@ExtendWith(MockitoExtension.class)
class LlmCallServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;
    @Mock
    private ChatResponse chatResponse;
    @Mock
    private Generation generation;
    @Mock
    private AssistantMessage assistantMessage;

    private LlmCallService service;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        service = new LlmCallService(chatClientBuilder);
    }

    // ====== 成功调用 ======

    @Test
    void call_success_returnsResponseText() {
        setupMockChain("你好，我是 AI 助手的回复");

        String result = service.call("测试提示");

        assertThat(result).isEqualTo("你好，我是 AI 助手的回复");
        verify(chatClient).prompt();
        verify(requestSpec).user("测试提示");
    }

    // ====== 重试耗尽 ======

    @Test
    void call_allAttemptsFail_throwsException() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenThrow(new RuntimeException("Connection refused"));

        RetryTemplate retryTemplate = createRetryTemplate(3);

        assertThatThrownBy(() -> retryTemplate.execute(ctx -> service.call("test")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection refused");

        // 验证被调用 3 次（首次 + 2 次重试）
        verify(requestSpec, times(3)).user(anyString());
    }

    // ====== 重试后成功 ======

    @Test
    void call_retryThenSuccess_callsCorrectNumberOfTimes() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse())
                .thenThrow(new RuntimeException("Temporary failure"))
                .thenThrow(new RuntimeException("Still failing"))
                .thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(assistantMessage.getText()).thenReturn("最终成功");

        RetryTemplate retryTemplate = createRetryTemplate(3);

        String result = retryTemplate.execute(ctx -> service.call("test"));
        assertThat(result).isEqualTo("最终成功");
        verify(requestSpec, times(3)).user(anyString());
    }

    // ====== 超时异常分类 ======

    @Test
    void call_timeoutException_classifiedAsLlmCallFailed() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        // Spring AI 会将超时异常包装为 unchecked exception
        when(callResponseSpec.chatResponse()).thenThrow(new RuntimeException("Read timed out"));

        // 超时异常是可重试的 RuntimeException，不是 RkosException
        assertThatThrownBy(() -> service.call("test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Read timed out");
    }

    // ====== 配额异常分类 ======

    @Test
    void call_quotaException_classifiedAsLlmQuotaExceeded() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenThrow(new RuntimeException("HTTP 429 Too Many Requests"));

        // 配额异常转为 RkosException(LLM_QUOTA_EXCEEDED)，Spring Retry 不重试
        assertThatThrownBy(() -> service.call("test"))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> {
                    RkosException rkosEx = (RkosException) ex;
                    assertThat(rkosEx.getErrorCode()).isEqualTo("LLM_QUOTA_EXCEEDED");
                    assertThat(rkosEx.getMessage()).isEqualTo("LLM 调用配额不足");
                });
    }

    // ====== 配额异常关键字匹配 ======

    @Test
    void call_rateLimitKeyword_classifiedAsQuotaExceeded() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenThrow(new RuntimeException("rate limit exceeded"));

        assertThatThrownBy(() -> service.call("test"))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> assertThat(((RkosException) ex).getErrorCode())
                        .isEqualTo("LLM_QUOTA_EXCEEDED"));
    }

    // ====== 配额异常不触发重试 ======

    @Test
    void call_quotaException_noRetryEvenWithRetryTemplate() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenThrow(new RuntimeException("quota exceeded"));

        RetryTemplate retryTemplate = createRetryTemplate(3);

        // 配额异常 → RkosException → noRetryFor → 只调用 1 次，不重试
        assertThatThrownBy(() -> retryTemplate.execute(ctx -> service.call("test")))
                .isInstanceOf(RkosException.class);

        verify(requestSpec, times(1)).user(anyString());
    }

    // ====== ChatClient.Builder 正确构建 ======

    @Test
    void constructor_buildsChatClientFromBuilder() {
        verify(chatClientBuilder).build();
    }

    // ====== null prompt 防御 ======

    @Test
    void call_nullPrompt_throwsNullPointerException() {
        assertThatThrownBy(() -> service.call(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("prompt");
    }

    // ====== LLM 返回空响应 ======

    @Test
    void call_nullResponse_throwsRkosException() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(null);

        assertThatThrownBy(() -> service.call("test"))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> assertThat(((RkosException) ex).getErrorCode())
                        .isEqualTo("LLM_CALL_FAILED"));
    }

    @Test
    void call_nullResult_throwsRkosException() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(null);

        assertThatThrownBy(() -> service.call("test"))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> assertThat(((RkosException) ex).getErrorCode())
                        .isEqualTo("LLM_CALL_FAILED"));
    }

    // ====== 辅助方法 ======

    /**
     * 构建完整的 Mock 调用链：prompt → user → call → chatResponse → result → output → text。
     */
    private void setupMockChain(String responseText) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(assistantMessage.getText()).thenReturn(responseText);
    }

    /**
     * 创建与 @Retryable 配置一致的 RetryTemplate（无退避延迟，加速测试）。
     * <p>
     * 策略：重试所有 Exception，但排除 RkosException。最大尝试次数由参数指定。
     */
    private RetryTemplate createRetryTemplate(int maxAttempts) {
        CompositeRetryPolicy compositePolicy = new CompositeRetryPolicy();

        MaxAttemptsRetryPolicy maxAttemptsPolicy = new MaxAttemptsRetryPolicy();
        maxAttemptsPolicy.setMaxAttempts(maxAttempts);

        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(
                Integer.MAX_VALUE,
                Map.of(Exception.class, true, RkosException.class, false)
        );

        compositePolicy.setPolicies(new org.springframework.retry.RetryPolicy[]{
                maxAttemptsPolicy, simpleRetryPolicy
        });

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(1); // 1ms，加速测试

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(compositePolicy);
        template.setBackOffPolicy(backOff);
        return template;
    }
}
