package com.rkos.modules.story.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 故事详情查询响应 DTO。
 * <p>
 * 用于 GET /api/v1/stories/{storyId} 返回故事完整信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "故事详情响应")
public class StoryDetailResponse {

    @Schema(description = "故事业务唯一标识（UUID）", example = "550e8400-e29b-41d4-a716-446655440000")
    private String storyId;

    @Schema(description = "故事内容", example = "我和她是在图书馆认识的...")
    private String content;

    @Schema(description = "关系类型", example = "爱情")
    private String relationshipType;

    @Schema(description = "是否匿名", example = "false")
    private Boolean anonymous;

    @Schema(description = "Agent 处理状态", example = "PENDING")
    private String processingStatus;

    @Schema(description = "内容长度", example = "128")
    private Integer contentLength;

    @Schema(description = "创建时间", example = "2025-07-16T10:30:00")
    private LocalDateTime createdAt;

    // ─── 确认摘要字段（FR10）— 仅 processingStatus=COMPLETED 时有值 ───

    @Schema(description = "关系类型（Agent 抽取）", example = "情侣")
    private String genomeRelationshipType;

    @Schema(description = "参与者数量", example = "2")
    private Integer participantCount;

    @Schema(description = "关键事件数量", example = "3")
    private Integer keyEventCount;

    @Schema(description = "整体置信度", example = "0.85")
    private BigDecimal overallConfidence;
}
