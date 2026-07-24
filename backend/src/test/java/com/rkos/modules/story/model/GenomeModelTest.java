package com.rkos.modules.story.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Genome 模型类单元测试。
 * <p>
 * 验证：
 * 1. 所有模型类的 Builder/Getter 正确工作
 * 2. GenomeData 9 维度完整构建
 * 3. Jackson 2.x JSON 序列化/反序列化往返（模拟 JSONB 读写）
 * 4. 扁平化列与 JSONB 内部字段同步逻辑
 */
class GenomeModelTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void setUpMapper() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    // ====== 各维度模型 Builder/Getter 测试 ======

    @Test
    void relationship_builderAndGetters_work() {
        Relationship rel = Relationship.builder()
                .type("情侣")
                .duration("3年")
                .stage("冷淡期")
                .startContext("大学校园")
                .build();

        assertThat(rel.getType()).isEqualTo("情侣");
        assertThat(rel.getDuration()).isEqualTo("3年");
        assertThat(rel.getStage()).isEqualTo("冷淡期");
        assertThat(rel.getStartContext()).isEqualTo("大学校园");
    }

    @Test
    void participant_builderAndGetters_work() {
        Participant p = Participant.builder()
                .role("叙述者")
                .attachment("焦虑型")
                .behaviors(List.of("索取确认", "频繁追问"))
                .emotions(List.of("焦虑", "不安"))
                .ageAtStory(25)
                .gender("female")
                .build();

        assertThat(p.getRole()).isEqualTo("叙述者");
        assertThat(p.getAttachment()).isEqualTo("焦虑型");
        assertThat(p.getBehaviors()).containsExactly("索取确认", "频繁追问");
        assertThat(p.getEmotions()).containsExactly("焦虑", "不安");
        assertThat(p.getAgeAtStory()).isEqualTo(25);
        assertThat(p.getGender()).isEqualTo("female");
    }

    @Test
    void keyEvent_builderAndGetters_work() {
        KeyEvent ke = KeyEvent.builder()
                .event("工作压力增加")
                .position("beginning")
                .description("叙述者开始频繁加班")
                .build();

        assertThat(ke.getEvent()).isEqualTo("工作压力增加");
        assertThat(ke.getPosition()).isEqualTo("beginning");
        assertThat(ke.getDescription()).isEqualTo("叙述者开始频繁加班");
    }

    @Test
    void conflictPattern_builderAndGetters_work() {
        ConflictPattern cp = ConflictPattern.builder()
                .type("communication")
                .frequency("recurring")
                .resolution("escalation")
                .description("沟通不畅导致争吵升级")
                .build();

        assertThat(cp.getType()).isEqualTo("communication");
        assertThat(cp.getFrequency()).isEqualTo("recurring");
        assertThat(cp.getResolution()).isEqualTo("escalation");
        assertThat(cp.getDescription()).isEqualTo("沟通不畅导致争吵升级");
    }

    @Test
    void outcome_builderAndGetters_work() {
        Outcome o = Outcome.builder()
                .type("分手")
                .initiator("B")
                .manner("direct")
                .build();

        assertThat(o.getType()).isEqualTo("分手");
        assertThat(o.getInitiator()).isEqualTo("B");
        assertThat(o.getManner()).isEqualTo("direct");
    }

    @Test
    void confidence_builderAndGetters_work() {
        Confidence c = Confidence.builder()
                .overall(new BigDecimal("0.85"))
                .relationship(new BigDecimal("0.90"))
                .participants(new BigDecimal("0.80"))
                .causalChain(new BigDecimal("0.75"))
                .conflictPatterns(new BigDecimal("0.88"))
                .build();

        assertThat(c.getOverall()).isEqualByComparingTo("0.85");
        assertThat(c.getRelationship()).isEqualByComparingTo("0.90");
        assertThat(c.getParticipants()).isEqualByComparingTo("0.80");
        assertThat(c.getCausalChain()).isEqualByComparingTo("0.75");
        assertThat(c.getConflictPatterns()).isEqualByComparingTo("0.88");
    }

    @Test
    void emotionalArc_builderAndGetters_work() {
        EmotionalArc ea = EmotionalArc.builder()
                .dominantEmotions(List.of("遗憾", "不舍"))
                .trajectory("decline")
                .build();

        assertThat(ea.getDominantEmotions()).containsExactly("遗憾", "不舍");
        assertThat(ea.getTrajectory()).isEqualTo("decline");
    }

    // ====== GenomeData 9 维度完整构建 ======

    @Test
    void genomeData_buildComplete_9Dimensions() {
        GenomeData data = buildFullGenomeData();

        assertThat(data.getGenomeId()).isEqualTo("genome-001");
        assertThat(data.getStoryId()).isEqualTo("story-001");
        assertThat(data.getVersion()).isEqualTo("v1.0");
        assertThat(data.getRelationship()).isNotNull();
        assertThat(data.getParticipants()).hasSize(2);
        assertThat(data.getKeyEvents()).hasSize(2);
        assertThat(data.getCausalChain()).hasSize(3);
        assertThat(data.getConflictPatterns()).hasSize(1);
        assertThat(data.getOutcome()).isNotNull();
        assertThat(data.getLessons()).hasSize(2);
        assertThat(data.getConfidence()).isNotNull();
        assertThat(data.getEmotionalArc()).isNotNull();
    }

    // ====== JSON 序列化/反序列化往返 ======

    @Test
    void genomeData_jsonRoundTrip_preservesAllFields() throws JsonProcessingException {
        GenomeData original = buildFullGenomeData();

        // 序列化 → 反序列化
        String json = mapper.writeValueAsString(original);
        GenomeData deserialized = mapper.readValue(json, GenomeData.class);

        // 验证所有维度完整保留
        assertThat(deserialized.getGenomeId()).isEqualTo(original.getGenomeId());
        assertThat(deserialized.getStoryId()).isEqualTo(original.getStoryId());
        assertThat(deserialized.getVersion()).isEqualTo(original.getVersion());

        // Relationship
        assertThat(deserialized.getRelationship().getType()).isEqualTo("情侣");
        assertThat(deserialized.getRelationship().getDuration()).isEqualTo("3年");

        // Participants
        assertThat(deserialized.getParticipants()).containsKeys("A", "B");
        assertThat(deserialized.getParticipants().get("A").getRole()).isEqualTo("叙述者");
        assertThat(deserialized.getParticipants().get("B").getRole()).isEqualTo("对方");

        // KeyEvents
        assertThat(deserialized.getKeyEvents()).hasSize(2);
        assertThat(deserialized.getKeyEvents().get(0).getPosition()).isEqualTo("beginning");

        // CausalChain (List<String>)
        assertThat(deserialized.getCausalChain()).containsExactly("工作压力增加", "沟通减少", "争吵升级");

        // ConflictPatterns
        assertThat(deserialized.getConflictPatterns()).hasSize(1);
        assertThat(deserialized.getConflictPatterns().get(0).getType()).isEqualTo("communication");

        // Outcome
        assertThat(deserialized.getOutcome().getType()).isEqualTo("分手");

        // Lessons (List<String>)
        assertThat(deserialized.getLessons()).containsExactly("沟通是关系的基础", "需要关注对方的情感需求");

        // Confidence
        assertThat(deserialized.getConfidence().getOverall()).isEqualByComparingTo("0.85");

        // EmotionalArc
        assertThat(deserialized.getEmotionalArc().getDominantEmotions()).containsExactly("遗憾", "不舍");
        assertThat(deserialized.getEmotionalArc().getTrajectory()).isEqualTo("decline");
    }

    @Test
    void genomeData_deserializeWithUnknownFields_ignored() throws JsonProcessingException {
        String jsonWithExtra = """
                {
                  "genomeId": "g1",
                  "storyId": "s1",
                  "version": "v1",
                  "unknownFutureField": "should be ignored",
                  "relationship": { "type": "友谊", "extraField": 42 }
                }
                """;

        GenomeData data = mapper.readValue(jsonWithExtra, GenomeData.class);

        assertThat(data.getGenomeId()).isEqualTo("g1");
        assertThat(data.getRelationship().getType()).isEqualTo("友谊");
    }

    @Test
    void genomeData_nullFields_serializeAndDeserialize() throws JsonProcessingException {
        GenomeData data = GenomeData.builder()
                .genomeId("g2")
                .storyId("s2")
                .version("v1")
                .build();

        String json = mapper.writeValueAsString(data);
        GenomeData deserialized = mapper.readValue(json, GenomeData.class);

        assertThat(deserialized.getGenomeId()).isEqualTo("g2");
        assertThat(deserialized.getRelationship()).isNull();
        assertThat(deserialized.getParticipants()).isNull();
        assertThat(deserialized.getCausalChain()).isNull();
    }

    // ====== 扁平化列同步逻辑 ======

    @Test
    void relationshipGenome_flatColumnsSync_fromGenomeData() {
        GenomeData data = buildFullGenomeData();

        RelationshipGenome genome = RelationshipGenome.builder()
                .storyId(data.getStoryId())
                .agentVersion("v1.0")
                .genomeData(data)
                .relationshipType(data.getRelationship() != null ? data.getRelationship().getType() : null)
                .outcomeType(data.getOutcome() != null ? data.getOutcome().getType() : null)
                .overallConfidence(data.getConfidence() != null ? data.getConfidence().getOverall() : null)
                .build();

        assertThat(genome.getRelationshipType()).isEqualTo("情侣");
        assertThat(genome.getOutcomeType()).isEqualTo("分手");
        assertThat(genome.getOverallConfidence()).isEqualByComparingTo("0.85");
    }

    @Test
    void relationshipGenome_flatColumnsSync_nullSafe() {
        GenomeData data = GenomeData.builder()
                .genomeId("g3")
                .storyId("s3")
                .version("v1")
                .build();

        RelationshipGenome genome = RelationshipGenome.builder()
                .storyId(data.getStoryId())
                .agentVersion("v1.0")
                .genomeData(data)
                .relationshipType(data.getRelationship() != null ? data.getRelationship().getType() : null)
                .outcomeType(data.getOutcome() != null ? data.getOutcome().getType() : null)
                .overallConfidence(data.getConfidence() != null ? data.getConfidence().getOverall() : null)
                .build();

        assertThat(genome.getRelationshipType()).isNull();
        assertThat(genome.getOutcomeType()).isNull();
        assertThat(genome.getOverallConfidence()).isNull();
    }

    // ====== 辅助方法 ======

    /**
     * 构建完整的 GenomeData（9 维度），用于各项测试。
     */
    static GenomeData buildFullGenomeData() {
        return GenomeData.builder()
                .genomeId("genome-001")
                .storyId("story-001")
                .version("v1.0")
                .relationship(Relationship.builder()
                        .type("情侣")
                        .duration("3年")
                        .stage("冷淡期")
                        .startContext("大学校园")
                        .build())
                .participants(Map.of(
                        "A", Participant.builder()
                                .role("叙述者")
                                .attachment("焦虑型")
                                .behaviors(List.of("索取确认", "频繁追问"))
                                .emotions(List.of("焦虑", "不安"))
                                .ageAtStory(25)
                                .gender("female")
                                .build(),
                        "B", Participant.builder()
                                .role("对方")
                                .attachment("回避型")
                                .behaviors(List.of("沉默", "回避冲突"))
                                .emotions(List.of("疲惫", "无奈"))
                                .ageAtStory(27)
                                .gender("male")
                                .build()
                ))
                .keyEvents(List.of(
                        KeyEvent.builder()
                                .event("工作压力增加")
                                .position("beginning")
                                .description("叙述者开始频繁加班")
                                .build(),
                        KeyEvent.builder()
                                .event("争吵爆发")
                                .position("climax")
                                .description("一次激烈争吵后冷战")
                                .build()
                ))
                .causalChain(List.of("工作压力增加", "沟通减少", "争吵升级"))
                .conflictPatterns(List.of(
                        ConflictPattern.builder()
                                .type("communication")
                                .frequency("recurring")
                                .resolution("escalation")
                                .description("沟通不畅导致争吵升级")
                                .build()
                ))
                .outcome(Outcome.builder()
                        .type("分手")
                        .initiator("B")
                        .manner("direct")
                        .build())
                .lessons(List.of("沟通是关系的基础", "需要关注对方的情感需求"))
                .confidence(Confidence.builder()
                        .overall(new BigDecimal("0.85"))
                        .relationship(new BigDecimal("0.90"))
                        .participants(new BigDecimal("0.80"))
                        .causalChain(new BigDecimal("0.75"))
                        .conflictPatterns(new BigDecimal("0.88"))
                        .build())
                .emotionalArc(EmotionalArc.builder()
                        .dominantEmotions(List.of("遗憾", "不舍"))
                        .trajectory("decline")
                        .build())
                .build();
    }
}
