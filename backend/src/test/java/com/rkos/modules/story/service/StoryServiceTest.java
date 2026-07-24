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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link StoryService} 单元测试。
 * <p>
 * 使用 Mockito Mock StoryMongoRepository，验证 Service 层业务逻辑。
 */
@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock
    private StoryMongoRepository storyMongoRepository;

    @Mock
    private StoryProcessingService storyProcessingService;

    @Mock
    private GenomeMapper genomeMapper;

    @InjectMocks
    private StoryService storyService;

    @Test
    void submitStory_shouldGenerateStoryIdAndSave() {
        StoryRequest request = new StoryRequest();
        request.setContent("这是一段测试故事");
        request.setRelationshipType("亲情");
        request.setAnonymous(false);

        when(storyMongoRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoryResponse response = storyService.submitStory(request);

        assertThat(response.getStoryId()).isNotNull().isNotEmpty();
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getProcessingStatus()).isEqualTo("PROCESSING");

        // 验证异步处理被触发
        verify(storyProcessingService).processStoryAsync(response.getStoryId(), "这是一段测试故事");

        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyMongoRepository).save(captor.capture());
        Story savedStory = captor.getValue();

        assertThat(savedStory.getStoryId()).isEqualTo(response.getStoryId());
        assertThat(savedStory.getContent()).isEqualTo("这是一段测试故事");
        assertThat(savedStory.getRelationshipType()).isEqualTo("亲情");
        assertThat(savedStory.getAnonymous()).isFalse();
        assertThat(savedStory.getContentLength()).isEqualTo(8);
        assertThat(savedStory.getCreatedAt()).isNotNull();
        assertThat(savedStory.getUpdatedAt()).isNotNull();
    }

    @Test
    void submitStory_shouldCalculateContentLength() {
        StoryRequest request = new StoryRequest();
        request.setContent("Hello World");

        when(storyMongoRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        storyService.submitStory(request);

        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyMongoRepository).save(captor.capture());
        assertThat(captor.getValue().getContentLength()).isEqualTo(11);
    }

    @Test
    void submitStory_nullAnonymous_shouldDefaultToFalse() {
        StoryRequest request = new StoryRequest();
        request.setContent("测试内容");
        request.setAnonymous(null);

        when(storyMongoRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        storyService.submitStory(request);

        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyMongoRepository).save(captor.capture());
        assertThat(captor.getValue().getAnonymous()).isFalse();
    }

    @Test
    void submitStory_nullRelationshipType_shouldPassNull() {
        StoryRequest request = new StoryRequest();
        request.setContent("测试内容");

        when(storyMongoRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        storyService.submitStory(request);

        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyMongoRepository).save(captor.capture());
        assertThat(captor.getValue().getRelationshipType()).isNull();
    }

    @Test
    void submitStory_shouldUseBuilderDefaults() {
        StoryRequest request = new StoryRequest();
        request.setContent("测试");

        when(storyMongoRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        storyService.submitStory(request);

        ArgumentCaptor<Story> captor = ArgumentCaptor.forClass(Story.class);
        verify(storyMongoRepository).save(captor.capture());
        Story savedStory = captor.getValue();

        // Builder.Default 值由 Story 模型提供
        assertThat(savedStory.getStatus()).isEqualTo("ACTIVE");
        assertThat(savedStory.getProcessingStatus()).isEqualTo("PENDING");
        assertThat(savedStory.getVersion()).isEqualTo(1);
        assertThat(savedStory.getAttachments()).isEmpty();
    }

    // ═══════════════════════════════════════
    // Story 1-6：查询单元测试
    // ═══════════════════════════════════════

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, 7, 16, 12, 0, 0);

    @Test
    void getStoryByStoryId_shouldReturnDetailResponse() {
        Story story = Story.builder()
                .storyId("uuid-123")
                .content("测试故事")
                .relationshipType("亲情")
                .anonymous(false)
                .processingStatus("PENDING")
                .contentLength(4)
                .createdAt(FIXED_TIME)
                .build();
        when(storyMongoRepository.findByStoryId("uuid-123")).thenReturn(Optional.of(story));

        StoryDetailResponse response = storyService.getStoryByStoryId("uuid-123");

        assertThat(response.getStoryId()).isEqualTo("uuid-123");
        assertThat(response.getContent()).isEqualTo("测试故事");
        assertThat(response.getRelationshipType()).isEqualTo("亲情");
        assertThat(response.getAnonymous()).isFalse();
        assertThat(response.getProcessingStatus()).isEqualTo("PENDING");
        assertThat(response.getContentLength()).isEqualTo(4);
        assertThat(response.getCreatedAt()).isEqualTo(FIXED_TIME);
        // 非 COMPLETED 状态，确认摘要字段应为 null
        assertThat(response.getGenomeRelationshipType()).isNull();
        assertThat(response.getParticipantCount()).isNull();
        assertThat(response.getKeyEventCount()).isNull();
        assertThat(response.getOverallConfidence()).isNull();
        // 不应查询 Genome
        verify(genomeMapper, never()).selectByStoryId(any());
    }

    @Test
    void getStoryByStoryId_notFound_shouldThrowRkosException() {
        when(storyMongoRepository.findByStoryId("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storyService.getStoryByStoryId("non-existent"))
                .isInstanceOf(RkosException.class)
                .hasFieldOrPropertyWithValue("errorCode", "NOT_FOUND")
                .hasMessage("故事不存在");
    }

    @Test
    void getStories_shouldReturnPageResponse() {
        Story story = Story.builder()
                .storyId("uuid-1")
                .content("故事内容")
                .relationshipType("友情")
                .anonymous(false)
                .processingStatus("COMPLETED")
                .contentLength(4)
                .createdAt(FIXED_TIME)
                .build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Story> page = new PageImpl<>(List.of(story), pageable, 1);
        when(storyMongoRepository.findByRelationshipType("友情", pageable)).thenReturn(page);

        // COMPLETED 状态会查询 Genome，mock 返回 null（无 Genome 数据）
        when(genomeMapper.selectByStoryId("uuid-1")).thenReturn(null);

        StoryPageResponse response = storyService.getStories("友情", null, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStoryId()).isEqualTo("uuid-1");
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
    }

    @Test
    void getStories_noFilters_shouldReturnAllPaged() {
        Story story = Story.builder()
                .storyId("uuid-all")
                .content("全部内容")
                .anonymous(true)
                .processingStatus("PENDING")
                .contentLength(4)
                .createdAt(FIXED_TIME)
                .build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Story> page = new PageImpl<>(List.of(story), pageable, 1);
        when(storyMongoRepository.findAll(pageable)).thenReturn(page);

        StoryPageResponse response = storyService.getStories(null, null, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStoryId()).isEqualTo("uuid-all");
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
    }

    // ═══════════════════════════════════════
    // Story 2-6：确认摘要单元测试
    // ═══════════════════════════════════════

    @Test
    void getStoryByStoryId_completed_shouldReturnConfirmationSummary() {
        Story story = Story.builder()
                .storyId("uuid-completed")
                .content("已完成的故事")
                .relationshipType("爱情")
                .anonymous(false)
                .processingStatus("COMPLETED")
                .contentLength(6)
                .createdAt(FIXED_TIME)
                .build();
        when(storyMongoRepository.findByStoryId("uuid-completed")).thenReturn(Optional.of(story));

        // Mock Genome 数据
        GenomeData genomeData = GenomeData.builder()
                .participants(Map.of("A", new com.rkos.modules.story.model.Participant(),
                        "B", new com.rkos.modules.story.model.Participant()))
                .keyEvents(List.of(
                        new com.rkos.modules.story.model.KeyEvent(),
                        new com.rkos.modules.story.model.KeyEvent(),
                        new com.rkos.modules.story.model.KeyEvent()))
                .build();
        RelationshipGenome genome = RelationshipGenome.builder()
                .storyId("uuid-completed")
                .relationshipType("爱情")
                .overallConfidence(new java.math.BigDecimal("0.85"))
                .genomeData(genomeData)
                .build();
        when(genomeMapper.selectByStoryId("uuid-completed")).thenReturn(genome);

        StoryDetailResponse response = storyService.getStoryByStoryId("uuid-completed");

        assertThat(response.getProcessingStatus()).isEqualTo("COMPLETED");
        assertThat(response.getGenomeRelationshipType()).isEqualTo("爱情");
        assertThat(response.getParticipantCount()).isEqualTo(2);
        assertThat(response.getKeyEventCount()).isEqualTo(3);
        assertThat(response.getOverallConfidence()).isEqualByComparingTo(new java.math.BigDecimal("0.85"));
    }

    @Test
    void getStoryByStoryId_completed_genomeQueryFails_shouldReturnNullSummary() {
        Story story = Story.builder()
                .storyId("uuid-fail")
                .content("处理完成但查询失败")
                .processingStatus("COMPLETED")
                .contentLength(9)
                .createdAt(FIXED_TIME)
                .build();
        when(storyMongoRepository.findByStoryId("uuid-fail")).thenReturn(Optional.of(story));
        when(genomeMapper.selectByStoryId("uuid-fail")).thenThrow(new RuntimeException("DB error"));

        StoryDetailResponse response = storyService.getStoryByStoryId("uuid-fail");

        // 主查询不受影响，摘要字段为 null
        assertThat(response.getProcessingStatus()).isEqualTo("COMPLETED");
        assertThat(response.getGenomeRelationshipType()).isNull();
        assertThat(response.getParticipantCount()).isNull();
    }

    @Test
    void submitStory_shouldTriggerAsyncProcessing() {
        StoryRequest request = new StoryRequest();
        request.setContent("异步触发测试");

        when(storyMongoRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoryResponse response = storyService.submitStory(request);

        assertThat(response.getProcessingStatus()).isEqualTo("PROCESSING");
        verify(storyProcessingService).processStoryAsync(response.getStoryId(), "异步触发测试");
    }

    // ═══════════════════════════════════════
    // Story 2-7：重新处理单元测试
    // ═══════════════════════════════════════

    @Test
    void reprocessStory_normalFlow_shouldTriggerAsyncReprocess() {
        Story story = Story.builder()
                .storyId("uuid-reprocess")
                .content("重新处理的故事")
                .processingStatus("COMPLETED")
                .createdAt(FIXED_TIME)
                .build();
        when(storyMongoRepository.findByStoryId("uuid-reprocess")).thenReturn(Optional.of(story));

        StoryResponse response = storyService.reprocessStory("uuid-reprocess");

        assertThat(response.getStoryId()).isEqualTo("uuid-reprocess");
        assertThat(response.getCreatedAt()).isEqualTo(FIXED_TIME);
        assertThat(response.getProcessingStatus()).isEqualTo("REPROCESSING");
        verify(storyProcessingService).reprocessStoryAsync("uuid-reprocess", "重新处理的故事");
    }

    @Test
    void reprocessStory_storyNotFound_shouldThrowException() {
        when(storyMongoRepository.findByStoryId("not-found")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storyService.reprocessStory("not-found"))
                .isInstanceOf(RkosException.class)
                .hasFieldOrPropertyWithValue("errorCode", "NOT_FOUND")
                .hasMessage("故事不存在");

        verify(storyProcessingService, never()).reprocessStoryAsync(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void reprocessStory_statusProcessing_shouldThrowConflict() {
        Story story = Story.builder()
                .storyId("uuid-processing")
                .content("处理中的故事")
                .processingStatus("PROCESSING")
                .build();
        when(storyMongoRepository.findByStoryId("uuid-processing")).thenReturn(Optional.of(story));

        assertThatThrownBy(() -> storyService.reprocessStory("uuid-processing"))
                .isInstanceOf(RkosException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CONFLICT");

        verify(storyProcessingService, never()).reprocessStoryAsync(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void reprocessStory_statusReprocessing_shouldThrowConflict() {
        Story story = Story.builder()
                .storyId("uuid-reprocessing")
                .content("重新处理中的故事")
                .processingStatus("REPROCESSING")
                .build();
        when(storyMongoRepository.findByStoryId("uuid-reprocessing")).thenReturn(Optional.of(story));

        assertThatThrownBy(() -> storyService.reprocessStory("uuid-reprocessing"))
                .isInstanceOf(RkosException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CONFLICT");

        verify(storyProcessingService, never()).reprocessStoryAsync(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void reprocessStory_statusFailed_shouldAllowReprocess() {
        Story story = Story.builder()
                .storyId("uuid-failed")
                .content("失败的故事")
                .processingStatus("FAILED")
                .createdAt(FIXED_TIME)
                .build();
        when(storyMongoRepository.findByStoryId("uuid-failed")).thenReturn(Optional.of(story));

        StoryResponse response = storyService.reprocessStory("uuid-failed");

        assertThat(response.getProcessingStatus()).isEqualTo("REPROCESSING");
        verify(storyProcessingService).reprocessStoryAsync("uuid-failed", "失败的故事");
    }

    @Test
    void reprocessStory_statusPending_shouldAllowReprocess() {
        Story story = Story.builder()
                .storyId("uuid-pending")
                .content("待处理的故事")
                .processingStatus("PENDING")
                .createdAt(FIXED_TIME)
                .build();
        when(storyMongoRepository.findByStoryId("uuid-pending")).thenReturn(Optional.of(story));

        StoryResponse response = storyService.reprocessStory("uuid-pending");

        assertThat(response.getProcessingStatus()).isEqualTo("REPROCESSING");
        verify(storyProcessingService).reprocessStoryAsync("uuid-pending", "待处理的故事");
    }
}
