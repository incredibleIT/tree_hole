package com.rkos.modules.story.service;

import com.rkos.modules.story.agent.StoryUnderstandingAgent;
import com.rkos.modules.story.model.RelationshipGenome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 故事异步编排服务。
 * <p>
 * 在独立线程池中协调 Agent 分析与 Genome 持久化，
 * 通过 {@code @Async("storyAgentExecutor")} 实现异步执行。
 * <p>
 * 异常在方法内完全捕获，不传播到调用方。
 * <p>
 * 同时提供首次处理 {@link #processStoryAsync} 和重新处理 {@link #reprocessStoryAsync} 两个入口。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StoryProcessingService {

    private final StoryUnderstandingAgent storyUnderstandingAgent;
    private final StoryPersistenceService storyPersistenceService;

    /**
     * 异步处理故事：Agent 分析 + Genome 持久化。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>调用 {@link StoryUnderstandingAgent#analyzeStory(String, String)} 分析故事</li>
     *   <li>调用 {@link StoryPersistenceService#persistGenome(String, RelationshipGenome)} 持久化</li>
     * </ol>
     * <p>
     * 异常完全捕获，不抛出到调用方。{@link StoryPersistenceService} 内部已处理
     * MongoDB FAILED 状态更新，此处仅记录 ERROR 日志。
     *
     * @param storyId      故事 ID（UUID）
     * @param storyContent 故事文本内容
     */
    @Async("storyAgentExecutor")
    public void processStoryAsync(String storyId, String storyContent) {
        try {
            log.info("开始异步处理故事: storyId={}", storyId);

            // 1. Agent 分析
            RelationshipGenome genome = storyUnderstandingAgent.analyzeStory(storyContent, storyId);

            // 2. 持久化（内部处理 MongoDB 状态转换 PROCESSING → COMPLETED/FAILED）
            storyPersistenceService.persistGenome(storyId, genome);

            log.info("异步处理故事完成: storyId={}", storyId);
        } catch (Exception e) {
            // persistGenome 已处理 MongoDB FAILED 状态
            // 这里只兜底日志，不抛出（异步方法异常不传播到调用方）
            log.error("异步处理故事失败: storyId={}", storyId, e);
        }
    }

    /**
     * 异步重新处理故事：Agent 分析 + Genome 覆盖持久化。
     * <p>
     * 处理流程与 {@link #processStoryAsync} 相同，但持久化阶段调用
     * {@link StoryPersistenceService#repersistGenome} 覆盖旧 Genome。
     *
     * @param storyId      故事 ID（UUID）
     * @param storyContent 故事文本内容
     */
    @Async("storyAgentExecutor")
    public void reprocessStoryAsync(String storyId, String storyContent) {
        try {
            log.info("开始异步重新处理故事: storyId={}", storyId);

            // 1. Agent 分析
            RelationshipGenome genome = storyUnderstandingAgent.analyzeStory(storyContent, storyId);

            // 2. 覆盖持久化（内部处理 MongoDB 状态转换 REPROCESSING → COMPLETED/FAILED）
            storyPersistenceService.repersistGenome(storyId, genome);

            log.info("异步重新处理故事完成: storyId={}", storyId);
        } catch (Exception e) {
            // repersistGenome 已处理 MongoDB FAILED 状态
            log.error("异步重新处理故事失败: storyId={}", storyId, e);
        }
    }
}
