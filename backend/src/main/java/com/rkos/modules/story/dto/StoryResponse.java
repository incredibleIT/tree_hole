package com.rkos.modules.story.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 故事提交响应 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "故事提交响应")
public class StoryResponse {

    @Schema(description = "故事业务唯一标识（UUID）", example = "550e8400-e29b-41d4-a716-446655440000")
    private String storyId;

    @Schema(description = "故事创建时间", example = "2025-07-16T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Agent 处理状态", example = "PROCESSING")
    private String processingStatus;
}
