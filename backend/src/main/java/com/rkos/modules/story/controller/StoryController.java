package com.rkos.modules.story.controller;

import com.rkos.modules.story.dto.StoryDetailResponse;
import com.rkos.modules.story.dto.StoryPageResponse;
import com.rkos.modules.story.dto.StoryRequest;
import com.rkos.modules.story.dto.StoryResponse;
import com.rkos.modules.story.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 故事接口控制器。
 */
@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
@Tag(name = "故事接口", description = "故事的提交与查询")
public class StoryController {

    private final StoryService storyService;

    @PostMapping
    @Operation(summary = "提交故事", description = "提交一段文字故事，系统接收并存储。")
    public ResponseEntity<com.rkos.common.ApiResponse<StoryResponse>> submitStory(
            @Valid @RequestBody StoryRequest request) {
        StoryResponse response = storyService.submitStory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.rkos.common.ApiResponse.success(response));
    }

    @GetMapping("/{storyId}")
    @Operation(summary = "查询故事详情", description = "根据故事 ID 查询已提交的故事详情。")
    public ResponseEntity<com.rkos.common.ApiResponse<StoryDetailResponse>> getStory(
            @Parameter(description = "故事业务唯一标识", required = true)
            @PathVariable String storyId) {
        StoryDetailResponse response = storyService.getStoryByStoryId(storyId);
        return ResponseEntity.ok(com.rkos.common.ApiResponse.success(response));
    }

    @PostMapping("/{storyId}/reprocess")
    @Operation(summary = "重新处理故事",
            description = "对指定故事触发重新处理流程，Agent 重新分析并覆盖旧的 Genome。")
    public ResponseEntity<com.rkos.common.ApiResponse<StoryResponse>> reprocessStory(
            @Parameter(description = "故事业务唯一标识", required = true)
            @PathVariable String storyId) {
        StoryResponse response = storyService.reprocessStory(storyId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(com.rkos.common.ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "分页查询故事列表", description = "按条件过滤并分页查询故事列表。")
    public ResponseEntity<com.rkos.common.ApiResponse<StoryPageResponse>> getStories(
            @Parameter(description = "关系类型过滤（可选）")
            @RequestParam(required = false) String relationshipType,
            @Parameter(description = "处理状态过滤（可选）")
            @RequestParam(required = false) String processingStatus,
            @Parameter(description = "页码（从 0 开始，默认 0）")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小（默认 20）")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        StoryPageResponse response = storyService.getStories(relationshipType, processingStatus, pageable);
        return ResponseEntity.ok(com.rkos.common.ApiResponse.success(response));
    }
}
