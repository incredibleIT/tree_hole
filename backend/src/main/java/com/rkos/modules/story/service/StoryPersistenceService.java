package com.rkos.modules.story.service;

import com.rkos.common.RkosException;
import com.rkos.modules.story.mapper.GenomeMapper;
import com.rkos.modules.story.model.GenomeData;
import com.rkos.modules.story.model.RelationshipGenome;
import com.rkos.modules.story.model.Story;
import com.rkos.modules.story.repository.StoryMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 基因组持久化与处理状态管理服务。
 * <p>
 * 职责：Agent 分析完成后，将 {@link RelationshipGenome} 写入 PostgreSQL，
 * 同时协调更新 MongoDB 中故事的 {@code processing_status}。
 * <p>
 * 双数据库协调策略（应用层）：
 * <ol>
 *   <li>先更新 MongoDB → PROCESSING</li>
 *   <li>写入 PostgreSQL（GenomeMapper.insert）</li>
 *   <li>成功 → MongoDB → COMPLETED</li>
 *   <li>失败 → MongoDB → FAILED + errorMessage</li>
 * </ol>
 * <p>
 * 重新处理场景使用 {@link #repersistGenome(String, RelationshipGenome)}，
 * PostgreSQL 通过 upsert 覆盖旧记录。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StoryPersistenceService {

    private final GenomeMapper genomeMapper;
    private final StoryMongoRepository storyMongoRepository;

    private static final String AGENT_VERSION = "v1.0";
    private static final String MODEL_USED = "dashscope/qwen-max";

    /**
     * 持久化 Genome 到 PostgreSQL，同时更新 MongoDB 处理状态。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>输入校验（storyId、genome 非空）</li>
     *   <li>补全 {@link GenomeData#storyId}（Story 2-4 审查遗留修复）</li>
     *   <li>更新 MongoDB → PROCESSING，记录 startedAt</li>
     *   <li>写入 PostgreSQL（GenomeMapper.insert）</li>
     *   <li>成功 → MongoDB → COMPLETED，记录 completedAt、agentVersion、modelUsed</li>
     *   <li>失败 → MongoDB → FAILED，记录 errorMessage</li>
     * </ol>
     *
     * @param storyId 故事 ID（UUID）
     * @param genome  Agent 分析返回的 RelationshipGenome
     * @throws RkosException 当输入无效或持久化失败时抛出
     */
    public void persistGenome(String storyId, RelationshipGenome genome) {
        // 1. 输入校验
        validateInput(storyId, genome);

        LocalDateTime startedAt = LocalDateTime.now();

        // 2. 补全 GenomeData 内部字段（Story 2-4 审查遗留修复）
        supplementGenomeData(storyId, genome);

        // 3. 标记 MongoDB 为 PROCESSING
        updateProcessingStatus(storyId, "PROCESSING", startedAt, null);

        try {
            // 4. 设置时间戳（PostgreSQL 层）
            LocalDateTime now = LocalDateTime.now();
            if (genome.getCreatedAt() == null) {
                genome.setCreatedAt(now);
            }
            genome.setUpdatedAt(now);

            // 5. 写入 PostgreSQL
            genomeMapper.insert(genome);

            // 6. 标记 MongoDB 为 COMPLETED
            LocalDateTime completedAt = LocalDateTime.now();
            updateProcessingStatus(storyId, "COMPLETED", startedAt, completedAt);

            log.info("Genome 持久化成功: storyId={}", storyId);

        } catch (Exception e) {
            // 7. 标记 MongoDB 为 FAILED + 记录 errorMessage
            log.error("Genome 持久化失败: storyId={}", storyId, e);
            updateProcessingStatusFailed(storyId, startedAt, e.getMessage());
            throw new RkosException("GENOME_PERSIST_FAILED",
                    "Genome 持久化失败: " + storyId, e);
        }
    }

    /**
     * 重新持久化 Genome（重新处理场景）。
     * <p>
     * 与 {@link #persistGenome} 的区别：PostgreSQL 使用 upsert（覆盖旧记录）而非 insert。
     * 处理流程与 {@link #persistGenome} 一致，但 MongoDB 状态标记为 REPROCESSING。
     *
     * @param storyId 故事 ID（UUID）
     * @param genome  Agent 分析返回的 RelationshipGenome
     * @throws RkosException 当输入无效或持久化失败时抛出
     */
    public void repersistGenome(String storyId, RelationshipGenome genome) {
        // 1. 输入校验
        validateInput(storyId, genome);

        LocalDateTime startedAt = LocalDateTime.now();

        // 2. 补全 GenomeData 内部字段
        supplementGenomeData(storyId, genome);

        // 3. 标记 MongoDB 为 REPROCESSING（区别于首次的 PROCESSING）
        updateProcessingStatus(storyId, "REPROCESSING", startedAt, null);

        try {
            // 4. 设置时间戳（重新处理时 createdAt 也更新）
            LocalDateTime now = LocalDateTime.now();
            genome.setCreatedAt(now);
            genome.setUpdatedAt(now);

            // 5. PostgreSQL upsert（覆盖旧记录）
            genomeMapper.upsertByStoryId(genome);

            // 6. 标记 MongoDB 为 COMPLETED
            LocalDateTime completedAt = LocalDateTime.now();
            updateProcessingStatus(storyId, "COMPLETED", startedAt, completedAt);

            log.info("Genome 重新持久化成功: storyId={}", storyId);

        } catch (Exception e) {
            // 7. 标记 MongoDB 为 FAILED + 记录 errorMessage
            log.error("Genome 重新持久化失败: storyId={}", storyId, e);
            updateProcessingStatusFailed(storyId, startedAt, e.getMessage());
            throw new RkosException("GENOME_REPERSIST_FAILED",
                    "Genome 重新持久化失败: " + storyId, e);
        }
    }

    /**
     * 校验输入参数。
     *
     * @param storyId 故事 ID
     * @param genome  关系基因组
     * @throws RkosException 当输入为 null 或空白时抛出
     */
    private void validateInput(String storyId, RelationshipGenome genome) {
        if (storyId == null || storyId.isBlank()) {
            throw new RkosException("PERSIST_INVALID_INPUT", "storyId 不能为 null 或空白");
        }
        if (genome == null) {
            throw new RkosException("PERSIST_INVALID_INPUT", "genome 不能为 null");
        }
    }

    /**
     * 补全 GenomeData 内部的 storyId 字段。
     * <p>
     * Story 2-4 审查发现 LLM 解析后 {@link GenomeData#getStoryId()} 为 null，
     * 在此统一补全以保证 JSONB 内部 {@code story_id} 与顶层一致。
     *
     * @param storyId 故事 ID
     * @param genome  关系基因组
     */
    private void supplementGenomeData(String storyId, RelationshipGenome genome) {
        if (genome.getGenomeData() != null) {
            GenomeData data = genome.getGenomeData();
            if (data.getStoryId() == null) {
                data.setStoryId(storyId);
                log.debug("补全 GenomeData.storyId: storyId={}", storyId);
            }
            // genomeId 可选，不强制填充（LLM 可能不输出）
        }
    }

    /**
     * 更新 MongoDB 故事处理状态（成功/进行中场景）。
     * <p>
     * MongoDB 更新失败时记录 ERROR 日志但不中断主流程
     * （PostgreSQL 数据保留，由后续补偿机制处理）。
     *
     * @param storyId     故事 ID
     * @param status      处理状态
     * @param startedAt   开始时间
     * @param completedAt 完成时间（PROCESSING 阶段为 null）
     */
    private void updateProcessingStatus(String storyId, String status,
                                         LocalDateTime startedAt,
                                         LocalDateTime completedAt) {
        try {
            storyMongoRepository.findByStoryId(storyId).ifPresent(story -> {
                story.setProcessingStatus(status);
                story.setUpdatedAt(LocalDateTime.now());
                story.setProcessingMetadata(Story.ProcessingMetadata.builder()
                        .agentVersion(AGENT_VERSION)
                        .modelUsed(MODEL_USED)
                        .startedAt(startedAt)
                        .completedAt(completedAt)
                        .retryCount(0)
                        .build());
                storyMongoRepository.save(story);
            });
        } catch (Exception e) {
            // MongoDB 更新失败：PostgreSQL 数据保留，ERROR 日志记录
            // 由后续定时补偿任务（Story 2-7）处理
            log.error("MongoDB 处理状态更新失败（补偿机制）: storyId={}, status={}",
                    storyId, status, e);
            if ("PROCESSING".equals(status)) {
                // PROCESSING 阶段 MongoDB 不可用 → 不继续写入 PostgreSQL
                throw new RkosException("MONGO_UPDATE_FAILED",
                        "MongoDB 处理状态更新失败: " + storyId, e);
            }
            // COMPLETED 阶段 MongoDB 更新失败 → PostgreSQL 数据已写入，仅记日志
        }
    }

    /**
     * 更新 MongoDB 故事处理状态为 FAILED。
     * <p>
     * 记录失败原因到 {@code processing_metadata.errorMessage}，
     * 截断至 500 字符避免文档过大。
     *
     * @param storyId      故事 ID
     * @param startedAt    开始时间
     * @param errorMessage 错误信息
     */
    private void updateProcessingStatusFailed(String storyId,
                                               LocalDateTime startedAt,
                                               String errorMessage) {
        try {
            storyMongoRepository.findByStoryId(storyId).ifPresent(story -> {
                story.setProcessingStatus("FAILED");
                story.setUpdatedAt(LocalDateTime.now());
                story.setProcessingMetadata(Story.ProcessingMetadata.builder()
                        .agentVersion(AGENT_VERSION)
                        .modelUsed(MODEL_USED)
                        .startedAt(startedAt)
                        .completedAt(LocalDateTime.now())
                        .retryCount(0)
                        .errorMessage(truncateError(errorMessage, 500))
                        .build());
                storyMongoRepository.save(story);
            });
        } catch (Exception e) {
            // FAILED 状态更新也失败 → 仅记日志，由补偿机制处理
            log.error("MongoDB FAILED 状态更新也失败: storyId={}", storyId, e);
        }
    }

    /**
     * 截断错误信息至指定长度。
     *
     * @param error     原始错误信息
     * @param maxLength 最大长度
     * @return 截断后的错误信息
     */
    private String truncateError(String error, int maxLength) {
        if (error == null) {
            return null;
        }
        return error.length() > maxLength ? error.substring(0, maxLength) + "..." : error;
    }
}
