package com.rkos.modules.story.seed;

import com.rkos.modules.story.model.GenomeData;
import com.rkos.modules.story.model.RelationshipGenome;
import com.rkos.modules.story.model.Story;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SeedDataLoader 单元测试。
 * <p>
 * 验证种子数据 JSON 解析正确性、完整性、一致性。
 * 纯 JUnit 5 测试，不依赖 Spring 上下文。
 */
class SeedDataLoaderTest {

    private static List<RelationshipGenome> genomes;
    private static List<Story> stories;

    @BeforeAll
    static void loadSeedData() {
        genomes = SeedDataLoader.loadGenomes();
        stories = SeedDataLoader.loadStories();
    }

    // ====== Genome 种子数据解析 ======

    @Nested
    class GenomeTests {

        @Test
        void loadGenomes_returnsExactlyFive() {
            assertThat(genomes).hasSize(5);
        }

        @Test
        void loadGenomes_storyIds_matchCoverageMatrix() {
            List<String> storyIds = genomes.stream()
                    .map(RelationshipGenome::getStoryId)
                    .toList();
            assertThat(storyIds).containsExactly(
                    "seed-story-001",
                    "seed-story-002",
                    "seed-story-003",
                    "seed-story-004",
                    "seed-story-005"
            );
        }

        @Test
        void loadGenomes_relationshipTypes_coverAllFive() {
            List<String> types = genomes.stream()
                    .map(RelationshipGenome::getRelationshipType)
                    .toList();
            assertThat(types).containsExactly("情侣", "友谊", "家庭", "同事", "师生");
        }

        @Test
        void loadGenomes_outcomeTypes_coverAllFive() {
            List<String> types = genomes.stream()
                    .map(RelationshipGenome::getOutcomeType)
                    .toList();
            assertThat(types).containsExactly("分手", "和好", "持续", "疏远", "感恩");
        }

        @Test
        void loadGenomes_genomeData_nineDimensionsComplete() {
            for (RelationshipGenome genome : genomes) {
                GenomeData gd = genome.getGenomeData();
                assertThat(gd).as("genomeData for %s", genome.getStoryId()).isNotNull();
                assertThat(gd.getGenomeId()).isNotBlank();
                assertThat(gd.getStoryId()).isEqualTo(genome.getStoryId());
                assertThat(gd.getVersion()).isNotBlank();
                assertThat(gd.getRelationship()).isNotNull();
                assertThat(gd.getRelationship().getType()).isNotBlank();
                assertThat(gd.getParticipants()).isNotNull().containsKeys("A", "B");
                assertThat(gd.getKeyEvents()).isNotNull().isNotEmpty();
                assertThat(gd.getCausalChain()).isNotNull().isNotEmpty();
                assertThat(gd.getConflictPatterns()).isNotNull().isNotEmpty();
                assertThat(gd.getOutcome()).isNotNull();
                assertThat(gd.getOutcome().getType()).isNotBlank();
                assertThat(gd.getLessons()).isNotNull().isNotEmpty();
                assertThat(gd.getConfidence()).isNotNull();
                assertThat(gd.getConfidence().getOverall()).isNotNull();
                assertThat(gd.getEmotionalArc()).isNotNull();
                assertThat(gd.getEmotionalArc().getDominantEmotions()).isNotEmpty();
                assertThat(gd.getEmotionalArc().getTrajectory()).isNotBlank();
            }
        }

        @Test
        void loadGenomes_flatColumns_matchGenomeData() {
            for (RelationshipGenome genome : genomes) {
                GenomeData gd = genome.getGenomeData();
                assertThat(genome.getRelationshipType())
                        .as("relationshipType for %s", genome.getStoryId())
                        .isEqualTo(gd.getRelationship().getType());
                assertThat(genome.getOutcomeType())
                        .as("outcomeType for %s", genome.getStoryId())
                        .isEqualTo(gd.getOutcome().getType());
                assertThat(genome.getOverallConfidence())
                        .as("overallConfidence for %s", genome.getStoryId())
                        .isEqualByComparingTo(gd.getConfidence().getOverall());
            }
        }

        @Test
        void loadGenomes_agentVersion_isV1() {
            for (RelationshipGenome genome : genomes) {
                assertThat(genome.getAgentVersion()).isEqualTo("v1.0");
            }
        }

        @Test
        void loadGenome_firstRecord_coupleBreakup() {
            RelationshipGenome g = genomes.get(0);
            assertThat(g.getStoryId()).isEqualTo("seed-story-001");
            assertThat(g.getRelationshipType()).isEqualTo("情侣");
            assertThat(g.getOutcomeType()).isEqualTo("分手");
            assertThat(g.getOverallConfidence()).isEqualByComparingTo("0.85");

            GenomeData gd = g.getGenomeData();
            assertThat(gd.getRelationship().getDuration()).isEqualTo("3年");
            assertThat(gd.getRelationship().getStage()).isEqualTo("冷淡期");
            assertThat(gd.getOutcome().getInitiator()).isEqualTo("B");
            assertThat(gd.getEmotionalArc().getTrajectory()).isEqualTo("decline");
        }

        @Test
        void loadGenome_secondRecord_friendshipReconcile() {
            RelationshipGenome g = genomes.get(1);
            assertThat(g.getStoryId()).isEqualTo("seed-story-002");
            assertThat(g.getRelationshipType()).isEqualTo("友谊");
            assertThat(g.getOutcomeType()).isEqualTo("和好");
            assertThat(g.getOverallConfidence()).isEqualByComparingTo("0.78");
        }

        @Test
        void loadGenome_thirdRecord_familyOngoing() {
            RelationshipGenome g = genomes.get(2);
            assertThat(g.getStoryId()).isEqualTo("seed-story-003");
            assertThat(g.getRelationshipType()).isEqualTo("家庭");
            assertThat(g.getOutcomeType()).isEqualTo("持续");
            assertThat(g.getOverallConfidence()).isEqualByComparingTo("0.92");
        }

        @Test
        void loadGenome_fourthRecord_colleagueDrift() {
            RelationshipGenome g = genomes.get(3);
            assertThat(g.getStoryId()).isEqualTo("seed-story-004");
            assertThat(g.getRelationshipType()).isEqualTo("同事");
            assertThat(g.getOutcomeType()).isEqualTo("疏远");
            assertThat(g.getOverallConfidence()).isEqualByComparingTo("0.70");
        }

        @Test
        void loadGenome_fifthRecord_teacherGratitude() {
            RelationshipGenome g = genomes.get(4);
            assertThat(g.getStoryId()).isEqualTo("seed-story-005");
            assertThat(g.getRelationshipType()).isEqualTo("师生");
            assertThat(g.getOutcomeType()).isEqualTo("感恩");
            assertThat(g.getOverallConfidence()).isEqualByComparingTo("0.88");
        }
    }

    // ====== Story 种子数据解析 ======

    @Nested
    class StoryTests {

        @Test
        void loadStories_returnsExactlyFive() {
            assertThat(stories).hasSize(5);
        }

        @Test
        void loadStories_storyIds_matchGenomesOneToOne() {
            Set<String> genomeIds = genomes.stream()
                    .map(RelationshipGenome::getStoryId)
                    .collect(Collectors.toSet());
            Set<String> storyIds = stories.stream()
                    .map(Story::getStoryId)
                    .collect(Collectors.toSet());
            assertThat(storyIds).isEqualTo(genomeIds);
        }

        @Test
        void loadStories_allProcessingStatusCompleted() {
            for (Story story : stories) {
                assertThat(story.getProcessingStatus())
                        .as("processingStatus for %s", story.getStoryId())
                        .isEqualTo("COMPLETED");
            }
        }

        @Test
        void loadStories_allHaveContent() {
            for (Story story : stories) {
                assertThat(story.getContent())
                        .as("content for %s", story.getStoryId())
                        .isNotBlank();
            }
        }

        @Test
        void loadStories_allHaveProcessingMetadata() {
            for (Story story : stories) {
                assertThat(story.getProcessingMetadata())
                        .as("processingMetadata for %s", story.getStoryId())
                        .isNotNull();
                assertThat(story.getProcessingMetadata().getAgentVersion()).isEqualTo("v1.0");
                assertThat(story.getProcessingMetadata().getModelUsed()).isEqualTo("seed-data");
            }
        }

        @Test
        void loadStories_relationshipTypes_matchGenomes() {
            Map<String, String> genomeRelTypes = genomes.stream()
                    .collect(Collectors.toMap(RelationshipGenome::getStoryId,
                            RelationshipGenome::getRelationshipType));
            for (Story story : stories) {
                assertThat(story.getRelationshipType())
                        .as("relationshipType for %s", story.getStoryId())
                        .isEqualTo(genomeRelTypes.get(story.getStoryId()));
            }
        }

        @Test
        void loadStories_allActiveStatus() {
            for (Story story : stories) {
                assertThat(story.getStatus()).isEqualTo("ACTIVE");
            }
        }

        @Test
        void loadStories_allHaveCreatedAt() {
            for (Story story : stories) {
                assertThat(story.getCreatedAt())
                        .as("createdAt for %s", story.getStoryId())
                        .isNotNull();
            }
        }

        @Test
        void loadStories_contentLength_matchesContentLength() {
            for (Story story : stories) {
                assertThat(story.getContentLength())
                        .as("contentLength for %s", story.getStoryId())
                        .isEqualTo(story.getContent().length());
            }
        }
    }

    // ====== 异常路径 ======

    @Test
    void loadList_nonExistentFile_throwsIllegalStateException() throws Exception {
        // 通过反射调用 private loadList 方法，直接测试 SeedDataLoader 的异常路径
        Method loadList = SeedDataLoader.class.getDeclaredMethod("loadList", String.class, Class.class);
        loadList.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                loadList.invoke(null, "seed/nonexistent.json", RelationshipGenome.class);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("种子数据文件不存在");
    }
}
