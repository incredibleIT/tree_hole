package com.rkos.common;

import com.rkos.modules.story.controller.StoryController;
import com.rkos.modules.story.dto.StoryResponse;
import com.rkos.modules.story.service.StoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link GlobalExceptionHandler} 集成测试。
 * <p>
 * 使用 MockMvc 独立模式，仅测试 Web 层，不加载数据库或 Spring AI 自动配置。
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    /**
     * 内部测试用 Controller，用于触发 RkosException 和通用 Exception。
     */
    @RestController
    static class TestExceptionController {

        @GetMapping("/test/rkos-exception-not-found")
        public String throwNotFound() {
            throw new RkosException("NOT_FOUND", "资源不存在");
        }

        @GetMapping("/test/rkos-exception-unauthorized")
        public String throwUnauthorized() {
            throw new RkosException("UNAUTHORIZED", "未授权访问");
        }

        @GetMapping("/test/rkos-exception-conflict")
        public String throwConflict() {
            throw new RkosException("CONFLICT", "资源冲突");
        }

        @GetMapping("/test/generic-exception")
        public String throwGeneric() {
            throw new RuntimeException("模拟系统内部错误");
        }
    }

    @BeforeEach
    void setUp() {
        StoryService mockStoryService = mock(StoryService.class);
        StoryResponse mockResponse = StoryResponse.builder()
                .storyId("mock-uuid")
                .createdAt(LocalDateTime.now())
                .build();
        when(mockStoryService.submitStory(any())).thenReturn(mockResponse);

        StoryController storyController = new StoryController(mockStoryService);
        TestExceptionController testController = new TestExceptionController();
        mockMvc = MockMvcBuilders.standaloneSetup(storyController, testController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validRequest_shouldReturn201Response() throws Exception {
        mockMvc.perform(post("/api/v1/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "这是一段测试故事"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.storyId").value("mock-uuid"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void emptyContent_shouldReturnValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("参数校验失败"))
                .andExpect(jsonPath("$.data.content").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void missingContent_shouldReturnValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.content").exists());
    }

    @Test
    void invalidJson_shouldReturnBadRequest() throws Exception {
        // 非法 JSON 触发 HttpMessageNotReadableException，由专用 handler 返回 400
        mockMvc.perform(post("/api/v1/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    @Test
    void rkosException_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/test/rkos-exception-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("资源不存在"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void rkosException_unauthorized_shouldReturn401() throws Exception {
        mockMvc.perform(get("/test/rkos-exception-unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("未授权访问"));
    }

    @Test
    void rkosException_conflict_shouldReturn409() throws Exception {
        mockMvc.perform(get("/test/rkos-exception-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("资源冲突"));
    }

    @Test
    void genericException_shouldReturn500WithoutStacktrace() throws Exception {
        mockMvc.perform(get("/test/generic-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("系统内部错误"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
