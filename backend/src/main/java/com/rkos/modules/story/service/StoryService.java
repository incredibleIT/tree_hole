package com.rkos.modules.story.service;

import com.rkos.common.RkosException;
import com.rkos.modules.story.dto.StoryDetailResponse;
import com.rkos.modules.story.dto.StoryPageResponse;
import com.rkos.modules.story.dto.StoryRequest;
import com.rkos.modules.story.dto.StoryResponse;
import com.rkos.modules.story.mapper.GenomeMapper;
import com.rkos.modules.story.model.GenomeData;
import com.rkos.modules.story.model.RelationshipGenome;
import com.rkos.modules.story.model.Story;
import com.rkos.modules.story.repository.StoryMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 故事服务。
 * <p>
 * 负责故事提交、详情查询、分页列表查询、重新处理。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoryService {

    private final StoryMongoRepository storyMongoRepository;
    private final StoryProcessingService storyProcessingService;
    private final GenomeMapper genomeMapper;

    /**
     * 提交故事：生成 UUID storyId、设置默认值、计算 contentLength、保存到 MongoDB。
     *
     * @param request 故事提交请求
     * @return 包含 storyId 和 createdAt 的响应
     */
    public StoryResponse submitStory(StoryRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String storyId = UUID.randomUUID().toString();

        Story story = Story.builder()
                .storyId(storyId)
                .content(request.getContent())
                .relationshipType(request.getRelationshipType())
                .anonymous(request.getAnonymous() != null ? request.getAnonymous() : false)
                .contentLength(request.getContent().length())
                .createdAt(now)
                .updatedAt(now)
                .build();

        storyMongoRepository.save(story);

        // 异步触发 Agent 处理
        storyProcessingService.processStoryAsync(storyId, request.getContent());

        return StoryResponse.builder()
                .storyId(storyId)
                .createdAt(now)
                .processingStatus("PROCESSING")
                .build();
    }

    /**
     * 根据 storyId 查询故事详情。
     *
     * @param storyId 业务唯一标识
     * @return 故事详情 DTO
     * @throws RkosException 当故事不存在时抛出 NOT_FOUND
     */
    public StoryDetailResponse getStoryByStoryId(String storyId) {
        Story story = storyMongoRepository.findByStoryId(storyId)
                .orElseThrow(() -> new RkosException("NOT_FOUND", "故事不存在"));
        return toDetailResponse(story);
    }

    /**
     * 分页查询故事列表，支持 relationshipType 和 processingStatus 可选过滤。
     *
     * @param relationshipType  关系类型（可为 null）
     * @param processingStatus  处理状态（可为 null）
     * @param pageable          分页参数
     * @return 分页响应 DTO
     */
    public StoryPageResponse getStories(String relationshipType, String processingStatus, Pageable pageable) {
        Page<Story> page;

        if (relationshipType != null && processingStatus != null) {
            page = storyMongoRepository.findByRelationshipTypeAndProcessingStatus(
                    relationshipType, processingStatus, pageable);
        } else if (relationshipType != null) {
            page = storyMongoRepository.findByRelationshipType(relationshipType, pageable);
        } else if (processingStatus != null) {
            page = storyMongoRepository.findByProcessingStatus(processingStatus, pageable);
        } else {
            page = storyMongoRepository.findAll(pageable);
        }

        List<StoryDetailResponse> content = page.getContent().stream()
                .map(this::toDetailResponse)
                .toList();

        return StoryPageResponse.builder()
                .content(content)
                .totalCount(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }

    /**
     * 重新处理故事：校验 → 状态检查 → 异步触发 Agent 重新分析。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>查询 MongoDB 故事是否存在，不存在抛 NOT_FOUND</li>
     *   <li>检查 processingStatus，PROCESSING/REPROCESSING 不允许重复触发，抛 CONFLICT</li>
     *   <li>异步触发 Agent 重新分析 + 覆盖持久化</li>
     * </ol>
     *
     * @param storyId 故事 ID
     * @return 包含 processingStatus="REPROCESSING" 的响应
     * @throws RkosException NOT_FOUND(404) / CONFLICT(409)
     */
    public StoryResponse reprocessStory(String storyId) {
        // 1. 查询故事是否存在
        Story story = storyMongoRepository.findByStoryId(storyId)
                .orElseThrow(() -> new RkosException("NOT_FOUND", "故事不存在"));

        // 2. 检查处理状态（PROCESSING / REPROCESSING 不允许重复触发）
        String status = story.getProcessingStatus();
        if ("PROCESSING".equals(status) || "REPROCESSING".equals(status)) {
            throw new RkosException("CONFLICT",
                    "故事正在处理中，不允许重复触发重新处理");
        }

        // 3. 异步触发重新处理
        storyProcessingService.reprocessStoryAsync(storyId, story.getContent());

        return StoryResponse.builder()
                .storyId(storyId)
                .createdAt(story.getCreatedAt())
                .processingStatus("REPROCESSING")
                .build();
    }

    /**
     * Story 领域模型 → StoryDetailResponse DTO 转换。
     * <p>
     * 当 {@code processingStatus = "COMPLETED"} 时，从 PostgreSQL 查询 Genome 构建确认摘要。
     */
    private StoryDetailResponse toDetailResponse(Story story) {
        StoryDetailResponse.StoryDetailResponseBuilder builder = StoryDetailResponse.builder()
                .storyId(story.getStoryId())
                .content(story.getContent())
                .relationshipType(story.getRelationshipType())
                .anonymous(story.getAnonymous())
                .processingStatus(story.getProcessingStatus())
                .contentLength(story.getContentLength())
                .createdAt(story.getCreatedAt());

        // 确认摘要（FR10）：仅 COMPLETED 状态从 PostgreSQL 查询
        if ("COMPLETED".equals(story.getProcessingStatus())) {
            try {
                RelationshipGenome genome = genomeMapper.selectByStoryId(story.getStoryId());
                if (genome != null) {
                    builder.genomeRelationshipType(genome.getRelationshipType());
                    builder.overallConfidence(genome.getOverallConfidence());
                    if (genome.getGenomeData() != null) {
                        builder.participantCount(countParticipants(genome.getGenomeData()));
                        builder.keyEventCount(countKeyEvents(genome.getGenomeData()));
                    }
                }
            } catch (Exception e) {
                log.warn("查询 Genome 确认摘要失败: storyId={}", story.getStoryId(), e);
                // 不影响主查询，摘要字段保持 null
            }
        }

        return builder.build();
    }

    /**
     * 计算参与者数量。
     */
    private Integer countParticipants(GenomeData data) {
        if (data.getParticipants() == null) return 0;
        return data.getParticipants().size();
    }

    /**
     * 计算关键事件数量。
     */
    private Integer countKeyEvents(GenomeData data) {
        if (data.getKeyEvents() == null) return 0;
        return data.getKeyEvents().size();
    }
}
