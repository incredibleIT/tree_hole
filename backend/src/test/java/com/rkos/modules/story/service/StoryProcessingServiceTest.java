package com.rkos.modules.story.service;

import com.rkos.common.RkosException;
import com.rkos.modules.story.agent.StoryUnderstandingAgent;
import com.rkos.modules.story.model.RelationshipGenome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link StoryProcessingService} 单元测试。
 * <p>
 * 使用纯 Mockito，不启动 Spring 上下文。
 * {@code @Async} 方法直接同步调用，验证逻辑正确性。
 */
@ExtendWith(MockitoExtension.class)
class StoryProcessingServiceTest {

    @Mock
    private StoryUnderstandingAgent storyUnderstandingAgent;

    @Mock
    private StoryPersistenceService storyPersistenceService;

    @InjectMocks
    private StoryProcessingService storyProcessingService;

    @Test
    void processStoryAsync_normalFlow_shouldAnalyzeAndPersist() {
        RelationshipGenome genome = RelationshipGenome.builder()
                .storyId("uuid-1")
                .relationshipType("爱情")
                .build();
        when(storyUnderstandingAgent.analyzeStory("故事内容", "uuid-1")).thenReturn(genome);

        storyProcessingService.processStoryAsync("uuid-1", "故事内容");

        verify(storyUnderstandingAgent).analyzeStory("故事内容", "uuid-1");
        verify(storyPersistenceService).persistGenome("uuid-1", genome);
    }

    @Test
    void processStoryAsync_agentThrows_shouldNotPropagateException() {
        when(storyUnderstandingAgent.analyzeStory("异常故事", "uuid-2"))
                .thenThrow(new RkosException("AGENT_PARSE_FAILED", "解析失败"));

        // 不应抛出异常
        storyProcessingService.processStoryAsync("uuid-2", "异常故事");

        // Agent 异常后不应调用持久化
        verify(storyPersistenceService, never()).persistGenome(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void processStoryAsync_persistenceThrows_shouldNotPropagateException() {
        RelationshipGenome genome = RelationshipGenome.builder()
                .storyId("uuid-3")
                .build();
        when(storyUnderstandingAgent.analyzeStory("持久化失败", "uuid-3")).thenReturn(genome);
        doThrow(new RkosException("GENOME_PERSIST_FAILED", "持久化失败"))
                .when(storyPersistenceService).persistGenome("uuid-3", genome);

        // 不应抛出异常
        storyProcessingService.processStoryAsync("uuid-3", "持久化失败");

        verify(storyUnderstandingAgent).analyzeStory("持久化失败", "uuid-3");
        verify(storyPersistenceService).persistGenome("uuid-3", genome);
    }

    @Test
    void processStoryAsync_nullContent_agentHandlesValidation() {
        when(storyUnderstandingAgent.analyzeStory(null, "uuid-4"))
                .thenThrow(new RkosException("AGENT_INVALID_INPUT", "故事内容不能为空"));

        // Agent 校验异常应被捕获，不传播
        storyProcessingService.processStoryAsync("uuid-4", null);

        verify(storyPersistenceService, never()).persistGenome(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    // ═══════════════════════════════════════
    // Story 2-7：异步重新处理单元测试
    // ═══════════════════════════════════════

    @Test
    void reprocessStoryAsync_normalFlow_shouldAnalyzeAndRepersist() {
        RelationshipGenome genome = RelationshipGenome.builder()
                .storyId("uuid-r1")
                .relationshipType("爱情")
                .build();
        when(storyUnderstandingAgent.analyzeStory("重新处理内容", "uuid-r1")).thenReturn(genome);

        storyProcessingService.reprocessStoryAsync("uuid-r1", "重新处理内容");

        verify(storyUnderstandingAgent).analyzeStory("重新处理内容", "uuid-r1");
        verify(storyPersistenceService).repersistGenome("uuid-r1", genome);
    }

    @Test
    void reprocessStoryAsync_agentThrows_shouldNotPropagateException() {
        when(storyUnderstandingAgent.analyzeStory("异常故事", "uuid-r2"))
                .thenThrow(new RkosException("AGENT_PARSE_FAILED", "解析失败"));

        // 不应抛出异常
        storyProcessingService.reprocessStoryAsync("uuid-r2", "异常故事");

        // Agent 异常后不应调用持久化
        verify(storyPersistenceService, never()).repersistGenome(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reprocessStoryAsync_persistThrows_shouldNotPropagateException() {
        RelationshipGenome genome = RelationshipGenome.builder()
                .storyId("uuid-r3")
                .build();
        when(storyUnderstandingAgent.analyzeStory("持久化失败", "uuid-r3")).thenReturn(genome);
        org.mockito.Mockito.doThrow(new RkosException("GENOME_REPERSIST_FAILED", "重新持久化失败"))
                .when(storyPersistenceService).repersistGenome("uuid-r3", genome);

        // 不应抛出异常
        storyProcessingService.reprocessStoryAsync("uuid-r3", "持久化失败");

        verify(storyUnderstandingAgent).analyzeStory("持久化失败", "uuid-r3");
        verify(storyPersistenceService).repersistGenome("uuid-r3", genome);
    }

    @Test
    void reprocessStoryAsync_nullContent_agentHandlesValidation() {
        when(storyUnderstandingAgent.analyzeStory(null, "uuid-r4"))
                .thenThrow(new RkosException("AGENT_INVALID_INPUT", "故事内容不能为空"));

        // Agent 校验异常应被捕获，不传播
        storyProcessingService.reprocessStoryAsync("uuid-r4", null);

        verify(storyPersistenceService, never()).repersistGenome(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }
}
