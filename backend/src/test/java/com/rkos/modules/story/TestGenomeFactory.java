package com.rkos.modules.story;

import com.rkos.modules.story.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 测试用 Genome 数据工厂（共享工具类）。
 * <p>
 * 提供构建完整 / 简化 GenomeData 的辅助方法，
 * 消除 GenomeMapperTest / SeedDataLoaderTest 中的重复代码。
 * <p>
 * 本类为纯工具类，禁止实例化。
 */
public final class TestGenomeFactory {

    private TestGenomeFactory() {
    }

    /**
     * 构建完整 9 维度 GenomeData（情侣/分手场景，与 GenomeMapperTest 原有逻辑一致）。
     *
     * @param storyId 关联的 Story ID
     * @return 完整填充的 GenomeData
     */
    public static GenomeData buildFullGenomeData(String storyId) {
        return GenomeData.builder()
                .genomeId("test-genome-001")
                .storyId(storyId)
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

    /**
     * 构建简化版 GenomeData（仅含必填字段，其余维度留空）。
     *
     * @param storyId 关联的 Story ID
     * @return 简化填充的 GenomeData
     */
    public static GenomeData buildMinimalGenomeData(String storyId) {
        return GenomeData.builder()
                .genomeId("test-genome-minimal")
                .storyId(storyId)
                .version("v1.0")
                .relationship(Relationship.builder()
                        .type("测试关系")
                        .build())
                .outcome(Outcome.builder()
                        .type("持续")
                        .build())
                .confidence(Confidence.builder()
                        .overall(new BigDecimal("0.50"))
                        .build())
                .build();
    }
}
