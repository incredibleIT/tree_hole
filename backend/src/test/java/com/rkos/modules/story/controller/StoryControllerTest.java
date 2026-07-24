package com.rkos.modules.story.controller;

import com.rkos.common.GlobalExceptionHandler;
import com.rkos.common.RkosException;
import com.rkos.modules.story.dto.StoryDetailResponse;
import com.rkos.modules.story.dto.StoryPageResponse;
import com.rkos.modules.story.dto.StoryRequest;
import com.rkos.modules.story.dto.StoryResponse;
import com.rkos.modules.story.service.StoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link StoryController} 集成测试。
 * <p>
 * 使用 MockMvc 独立模式 + Mock StoryService，不加载 Spring 上下文。
 */
@ExtendWith(MockitoExtension.class)
class StoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StoryService storyService;

    @InjectMocks
    private StoryController storyController;

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, 7, 16, 12, 0, 0);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void submitStory_validRequest_shouldReturn201WithStoryIdAndCreatedAt() throws Exception {
        StoryResponse response = StoryResponse.builder()
                .storyId("test-uuid-123")
                .createdAt(FIXED_TIME)
                .build();
        when(storyService.submitStory(any(StoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "这是一段测试故事", "relationshipType": "亲情", "anonymous": false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.storyId").value("test-uuid-123"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void submitStory_contentOnly_shouldReturn201() throws Exception {
        StoryResponse response = StoryResponse.builder()
                .storyId("uuid-456")
                .createdAt(FIXED_TIME)
                .build();
        when(storyService.submitStory(any(StoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "仅包含内容的请求"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.storyId").value("uuid-456"));
    }

    @Test
    void submitStory_emptyContent_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("参数校验失败"))
                .andExpect(jsonPath("$.data.content").exists());
    }

    @Test
    void submitStory_missingContent_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"relationshipType": "友情"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.content").exists());
    }

    @Test
    void submitStory_contentExceedsMaxLength_shouldReturn400() throws Exception {
        String longContent = "故".repeat(10001);
        mockMvc.perform(post("/api/v1/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + longContent + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.content").exists());
    }

    @Test
    void submitStory_invalidJson_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    // ═══════════════════════════════════════
    // Story 1-6：查询 API 测试
    // ═══════════════════════════════════════

    @Test
    void getStory_shouldReturn200WithDetail() throws Exception {
        StoryDetailResponse detail = StoryDetailResponse.builder()
                .storyId("test-uuid-123")
                .content("这是一段测试故事")
                .relationshipType("亲情")
                .anonymous(false)
                .processingStatus("PENDING")
                .contentLength(8)
                .createdAt(FIXED_TIME)
                .build();
        when(storyService.getStoryByStoryId("test-uuid-123")).thenReturn(detail);

        mockMvc.perform(get("/api/v1/stories/test-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.storyId").value("test-uuid-123"))
                .andExpect(jsonPath("$.data.content").value("这是一段测试故事"))
                .andExpect(jsonPath("$.data.relationshipType").value("亲情"))
                .andExpect(jsonPath("$.data.anonymous").value(false))
                .andExpect(jsonPath("$.data.processingStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.contentLength").value(8))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void getStory_notFound_shouldReturn404() throws Exception {
        when(storyService.getStoryByStoryId("non-existent"))
                .thenThrow(new RkosException("NOT_FOUND", "故事不存在"));

        mockMvc.perform(get("/api/v1/stories/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("故事不存在"));
    }

    @Test
    void getStories_shouldReturn200WithPagination() throws Exception {
        StoryDetailResponse detail = StoryDetailResponse.builder()
                .storyId("uuid-1")
                .content("故事内容")
                .relationshipType("友情")
                .anonymous(false)
                .processingStatus("PENDING")
                .contentLength(4)
                .createdAt(FIXED_TIME)
                .build();
        StoryPageResponse pageResponse = StoryPageResponse.builder()
                .content(List.of(detail))
                .totalCount(1)
                .page(0)
                .size(20)
                .build();
        when(storyService.getStories(any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/stories")
                        .param("relationshipType", "友情")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].storyId").value("uuid-1"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }
}
