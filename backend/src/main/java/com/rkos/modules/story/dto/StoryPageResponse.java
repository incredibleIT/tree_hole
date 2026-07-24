package com.rkos.modules.story.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 故事分页列表响应 DTO。
 * <p>
 * 用于 GET /api/v1/stories 返回分页故事列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "故事分页列表响应")
public class StoryPageResponse {

    @Schema(description = "当前页故事列表")
    private List<StoryDetailResponse> content;

    @Schema(description = "总记录数", example = "42")
    private long totalCount;

    @Schema(description = "当前页码（从 0 开始）", example = "0")
    private int page;

    @Schema(description = "每页大小", example = "20")
    private int size;
}
