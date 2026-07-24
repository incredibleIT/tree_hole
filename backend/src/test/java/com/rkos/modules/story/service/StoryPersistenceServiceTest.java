package com.rkos.modules.story.service;

import com.rkos.common.RkosException;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link StoryPersistenceService} 单元测试。
 * <p>
 * 使用纯 Mockito Mock {@link GenomeMapper} 和 {@link StoryMongoRepository}，
 * 不启动 Spring 上下文（Spring Boot 4.x 下 flapdoodle 不兼容）。
 */
@ExtendWith(MockitoExtension.class)
class StoryPersistenceServiceTest {

    @Mock
    private GenomeMapper genomeMapper;

    @Mock
    private StoryMongoRepository storyMongoRepository;

    @InjectMocks
    private StoryPersistenceService storyPersistenceService;

    // ═══════════════════════════════════════
    // AC #8: 正常持久化流程
    // ═══════════════════════════════════════

    @Test
    void persistGenome_normalFlow_shouldSucceed() {
        // Arrange
        String storyId = "test-story-uuid";
        RelationshipGenome genome = buildTestGenome(storyId);

        // 每次 findByStoryId 返回新的 Story 副本（模拟生产环境每次查询返回新对象）
        when(storyMongoRepository.findByStoryId(storyId))
                .thenReturn(Optional.of(buildTestStory(storyId)))
                .thenReturn(Optional.of(buildTestStory(storyId)));
        when(storyMongoRepository.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));
        when(genomeMapper.insert(any(RelationshipGenome.class))).thenReturn(1);

        // Act
        storyPersistenceService.persistGenome(storyId, genome);

        // Assert — 验证 MongoDB 被调用 2 次（PROCESSING + COMPLETED）
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyMongoRepository, times(2)).save(storyCaptor.capture());

        // 第一次：PROCESSING
        Story processingSave = storyCaptor.getAllValues().get(0);
        assertThat(processingSave.getProcessingStatus()).isEqualTo("PROCESSING");
        assertThat(processingSave.getProcessingMetadata()).isNotNull();
        assertThat(processingSave.getProcessingMetadata().getStartedAt()).isNotNull();
        assertThat(processingSave.getProcessingMetadata().getCompletedAt()).isNull();

        // 第二次：COMPLETED
        Story completedSave = storyCaptor.getAllValues().get(1);
        assertThat(completedSave.getProcessingStatus()).isEqualTo("COMPLETED");
        assertThat(completedSave.getProcessingMetadata().getAgentVersion()).isEqualTo("v1.0");
        assertThat(completedSave.getProcessingMetadata().getModelUsed()).isEqualTo("dashscope/qwen-max");
        assertThat(completedSave.getProcessingMetadata().getCompletedAt()).isNotNull();
        assertThat(completedSave.getProcessingMetadata().getRetryCount()).isEqualTo(0);

        // 验证 PostgreSQL insert 被调用
        verify(genomeMapper).insert(genome);
    }

    // ═══════════════════════════════════════
    // AC #7: GenomeData.storyId 补全验证
    // ═══════════════════════════════════════

    @Test
    void persistGenome_shouldSupplementGenomeDataStoryId() {
        // Arrange — genomeData.storyId 为 null（Story 2-4 遗留场景）
        String storyId = "test-story-uuid";
        GenomeData data = GenomeData.builder().storyId(null).build();
        RelationshipGenome genome = RelationshipGenome.builder()
                .storyId(storyId)
                .agentVersion("v1.0")
                .genomeData(data)
                .build();
        Story story = buildTestStory(storyId);

        when(storyMongoRepository.findByStoryId(storyId)).thenReturn(Optional.of(story));
        when(storyMongoRepository.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));
        when(genomeMapper.insert(any(RelationshipGenome.class))).thenReturn(1);

        // Act
        storyPersistenceService.persistGenome(storyId, genome);

        // Assert — GenomeData.storyId 已被补全
        assertThat(genome.getGenomeData().getStoryId()).isEqualTo(storyId);
    }

    @Test
    void persistGenome_genomeDataStoryIdAlreadySet_shouldNotOverwrite() {
        // Arrange — genomeData.storyId 已有值
        String storyId = "test-story-uuid";
        GenomeData data = GenomeData.builder().storyId("existing-id").build();
        RelationshipGenome genome = RelationshipGenome.builder()
                .storyId(storyId)
                .agentVersion("v1.0")
                .genomeData(data)
                .build();
        Story story = buildTestStory(storyId);

        when(storyMongoRepository.findByStoryId(storyId)).thenReturn(Optional.of(story));
        when(storyMongoRepository.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));
        when(genomeMapper.insert(any(RelationshipGenome.class))).thenReturn(1);

        // Act
        storyPersistenceService.persistGenome(storyId, genome);

        // Assert — 不应覆盖已有值
        assertThat(genome.getGenomeData().getStoryId()).isEqualTo("existing-id");
    }

    @Test
    void persistGenome_nullGenomeData_shouldNotThrow() {
        // Arrange — genomeData 为 null
        String storyId = "test-story-uuid";
        RelationshipGenome genome = RelationshipGenome.builder()
                .storyId(storyId)
                .agentVersion("v1.0")
                .genomeData(null)
                .build();
        Story story = buildTestStory(storyId);

        when(storyMongoRepository.findByStoryId(storyId)).thenReturn(Optional.of(story));
        when(storyMongoRepository.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));
        when(genomeMapper.insert(any(RelationshipGenome.class))).thenReturn(1);

        // Act & Assert — 不应抛异常
        storyPersistenceService.persistGenome(storyId, genome);
        verify(genomeMapper).insert(genome);
    }

    // ═══════════════════════════════════════
    // AC #5, #6: PostgreSQL 写入失败降级
    // ═══════════════════════════════════════

    @Test
    void persistGenome_postgresInsertFails_shouldMarkFailed() {
        // Arrange
        String storyId = "test-story-uuid";
        RelationshipGenome genome = buildTestGenome(storyId);
        Story story = buildTestStory(storyId);

        when(storyMongoRepository.findByStoryId(storyId)).thenReturn(Optional.of(story));
        when(storyMongoRepository.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));
        when(genomeMapper.insert(any(RelationshipGenome.class)))
                .thenThrow(new RuntimeException("PostgreSQL connection refused"));

        // Act & Assert
        assertThatThrownBy(() -> storyPersistenceService.persistGenome(storyId, genome))
                .isInstanceOf(RkosException.class)
                .hasFieldOrPropertyWithValue("errorCode", "GENOME_PERSIST_FAILED")
                .hasMessageContaining("Genome 持久化失败");

        // 验证 MongoDB 被调用 2 次（PROCESSING + FAILED）
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyMongoRepository, times(2)).save(storyCaptor.capture());

        Story failedSave = storyCaptor.getAllValues().get(1);
        assertThat(failedSave.getProcessingStatus()).isEqualTo("FAILED");
        assertThat(failedSave.getProcessingMetadata().getErrorMessage())
                .contains("PostgreSQL connection refused");
        assertThat(failedSave.getProcessingMetadata().getCompletedAt()).isNotNull();
    }

    // ═══════════════════════════════════════
    // AC #6: MongoDB 更新失败日志（补偿场景）
    // ═══════════════════════════════════════

    @Test
    void persistGenome_mongoProcessingUpdateFails_shouldThrowBeforePostgres() {
        // Arrange — MongoDB PROCESSING 更新失败
        String storyId = "test-story-uuid";
        RelationshipGenome genome = buildTestGenome(storyId);

        when(storyMongoRepository.findByStoryId(storyId))
                .thenThrow(new RuntimeException("MongoDB unavailable"));

        // Act & Assert
        assertThatThrownBy(() -> storyPersistenceService.persistGenome(storyId, genome))
                .isInstanceOf(RkosException.class)
                .hasFieldOrPropertyWithValue("errorCode", "MONGO_UPDATE_FAILED");

        // PostgreSQL 不应被调用（MongoDB PROCESSING 阶段失败 → 不继续）
        verify(genomeMapper, never()).insert(any(RelationshipGenome.class));
    }

    @Test
    void persistGenome_mongoCompletedUpdateFails_postgresDataRetained() {
        // Arrange — PostgreSQL 写入成功，但 MongoDB COMPLETED 更新失败
        String storyId = "test-story-uuid";
        RelationshipGenome genome = buildTestGenome(storyId);
        Story story = buildTestStory(storyId);

        // 第一次 findByStoryId → PROCESSING 成功
        // 第二次 findByStoryId → COMPLETED 失败
        when(storyMongoRepository.findByStoryId(storyId))
                .thenReturn(Optional.of(story))
                .thenThrow(new RuntimeException("MongoDB write failed"));
        when(storyMongoRepository.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));
        when(genomeMapper.insert(any(RelationshipGenome.class))).thenReturn(1);

        // Act — 不应抛异常（COMPLETED 阶段 MongoDB 失败仅记日志）
        storyPersistenceService.persistGenome(storyId, genome);

        // Assert — PostgreSQL insert 仍然执行了
        verify(genomeMapper).insert(genome);
    }

    // ═══════════════════════════════════════
    // AC #8: 空输入校验
    // ═══════════════════════════════════════

    @Test
    void persistGenome_nullStoryId_shouldThrow() {
        RelationshipGenome genome = buildTestGenome("some-id");

        assertThatThrownBy(() -> storyPersistenceService.persistGenome(null, genome))
                .isInstanceOf(RkosException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PERSIST_INVALID_INPUT")
                .hasMessageContaining("storyId");
    }

    @Test
    void persistGenome_blankStoryId_shouldThrow() {
        RelationshipGenome genome = buildTestGenome("some-id");

        assertThatThrownBy(() -> storyPersistenceService.persistGenome("   ", genome))
                .isInstanceOf(RkosException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PERSIST_INVALID_INPUT")
                .hasMessageContaining("storyId");
    }

    @Test
    void persistGenome_nullGenome_shouldThrow() {
        assertThatThrownBy(() -> storyPersistenceService.persistGenome("some-id", null))
                .isInstanceOf(RkosException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PERSIST_INVALID_INPUT")
                .hasMessageContaining("genome");
    }

    // ═══════════════════════════════════════
    // 额外边界场景
    // ═══════════════════════════════════════

    @Test
    void persistGenome_shouldSetTimestampsOnGenome() {
        // Arrange
        String storyId = "test-story-uuid";
        RelationshipGenome genome = buildTestGenome(storyId);
        genome.setCreatedAt(null); // 模拟 Agent 未设置 createdAt
        Story story = buildTestStory(storyId);

        when(storyMongoRepository.findByStoryId(storyId)).thenReturn(Optional.of(story));
        when(storyMongoRepository.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));
        when(genomeMapper.insert(any(RelationshipGenome.class))).thenReturn(1);

        // Act
        storyPersistenceService.persistGenome(storyId, genome);

        // Assert — createdAt 和 updatedAt 被设置
        assertThat(genome.getCreatedAt()).isNotNull();
        assertThat(genome.getUpdatedAt()).isNotNull();
    }

    @Test
    void persistGenome_existingCreatedAt_shouldNotOverwrite() {
        // Arrange
        String storyId = "test-story-uuid";
        RelationshipGenome genome = buildTestGenome(storyId);
        java.time.LocalDateTime existingTime = java.time.LocalDateTime.of(2025, 1, 1, 0, 0);
        genome.setCreatedAt(existingTime);
        Story story = buildTestStory(storyId);

        when(storyMongoRepository.findByStoryId(storyId)).thenReturn(Optional.of(story));
        when(storyMongoRepository.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));
        when(genomeMapper.insert(any(RelationshipGenome.class))).thenReturn(1);

        // Act
        storyPersistenceService.persistGenome(storyId, genome);

        // Assert — createdAt 不被覆盖
        assertThat(genome.getCreatedAt()).isEqualTo(existingTime);
        // updatedAt 总是更新
        assertThat(genome.getUpdatedAt()).isNotNull();
    }

    @Test
    void persistGenome_storyNotFoundInMongo_shouldNotThrow() {
        // Arrange — MongoDB 中找不到 story（findByStoryId 返回 empty）
        String storyId = "missing-story-uuid";
        RelationshipGenome genome = buildTestGenome(storyId);

        when(storyMongoRepository.findByStoryId(storyId)).thenReturn(Optional.empty());
        when(genomeMapper.insert(any(RelationshipGenome.class))).thenReturn(1);

        // Act — 不应抛异常（ifPresent 不执行，但不报错）
        storyPersistenceService.persistGenome(storyId, genome);

        // Assert — PostgreSQL insert 仍然执行
        verify(genomeMapper).insert(genome);
    }

    @Test
    void persistGenome_errorMessageTruncated() {
        // Arrange — 超长错误信息
        String storyId = "test-story-uuid";
        RelationshipGenome genome = buildTestGenome(storyId);
        Story story = buildTestStory(storyId);
        String longError = "A".repeat(1000);

        when(storyMongoRepository.findByStoryId(storyId)).thenReturn(Optional.of(story));
        when(storyMongoRepository.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));
        when(genomeMapper.insert(any(RelationshipGenome.class)))
                .thenThrow(new RuntimeException(longError));

        // Act
        try {
            storyPersistenceService.persistGenome(storyId, genome);
        } catch (RkosException ignored) {
            // expected
        }

        // Assert — 错误信息被截断到 500 + "..."
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);
        verify(storyMongoRepository, times(2)).save(storyCaptor.capture());
        Story failedSave = storyCaptor.getAllValues().get(1);
        String errorMessage = failedSave.getProcessingMetadata().getErrorMessage();
        assertThat(errorMessage).hasSize(503); // 500 + "..."
        assertThat(errorMessage).endsWith("...");
    }

    // ═══════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════

    private RelationshipGenome buildTestGenome(String storyId) {
        return RelationshipGenome.builder()
                .storyId(storyId)
                .agentVersion("v1.0")
                .genomeData(GenomeData.builder()
                        .storyId(storyId)
                        .version("1.0")
                        .build())
                .relationshipType("友情")
                .outcomeType("温暖")
                .overallConfidence(new BigDecimal("0.85"))
                .build();
    }

    private Story buildTestStory(String storyId) {
        return Story.builder()
                .storyId(storyId)
                .content("这是一段测试故事")
                .processingStatus("PENDING")
                .build();
    }
}
