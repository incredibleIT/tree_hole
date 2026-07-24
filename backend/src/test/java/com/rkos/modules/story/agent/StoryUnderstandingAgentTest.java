package com.rkos.modules.story.agent;

import com.rkos.common.LlmCallService;
import com.rkos.common.PromptTemplateService;
import com.rkos.common.RkosException;
import com.rkos.modules.story.model.RelationshipGenome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

/**
 * {@link StoryUnderstandingAgent} 单元测试。
 * <p>
 * 使用纯 Mockito 测试，mock {@link LlmCallService} 和 {@link PromptTemplateService}，
 * 不需要 Spring 上下文，不需要真实 LLM 调用。
 */
@ExtendWith(MockitoExtension.class)
class StoryUnderstandingAgentTest {

    @Mock
    private LlmCallService llmCallService;
    @Mock
    private PromptTemplateService promptTemplateService;

    private StoryUnderstandingAgent agent;

    private static final String STORY_ID = "story-uuid-12345";
    private static final String STORY_CONTENT = "我和她是在大学认识的...";
    private static final String SYSTEM_PROMPT = "你是一个专业的关系知识抽取专家...";
    private static final String USER_TEMPLATE = "请分析以下故事...\n{story_content}\n...";

    @BeforeEach
    void setUp() {
        agent = new StoryUnderstandingAgent(llmCallService, promptTemplateService);
    }

    // ====== 正常完整 Genome 解析 ======

    @Test
    void analyzeStory_fullGenome_parsesCorrectly() {
        setupPromptMocks();
        when(llmCallService.call(anyString())).thenReturn(buildFullGenomeJson());

        RelationshipGenome genome = agent.analyzeStory(STORY_CONTENT, STORY_ID);

        // 验证返回对象不为 null
        assertThat(genome).isNotNull();
        assertThat(genome.getStoryId()).isEqualTo(STORY_ID);
        assertThat(genome.getAgentVersion()).isEqualTo("v1.0");
        assertThat(genome.getGenomeData()).isNotNull();

        // 验证 9 个维度
        assertThat(genome.getGenomeData().getRelationship()).isNotNull();
        assertThat(genome.getGenomeData().getRelationship().getType()).isEqualTo("情侣");
        assertThat(genome.getGenomeData().getRelationship().getDuration()).isEqualTo("3年");
        assertThat(genome.getGenomeData().getRelationship().getStage()).isEqualTo("冷淡期");
        assertThat(genome.getGenomeData().getRelationship().getStartContext()).isEqualTo("大学校园");

        assertThat(genome.getGenomeData().getParticipants()).isNotNull();
        assertThat(genome.getGenomeData().getParticipants()).containsKeys("A", "B");
        assertThat(genome.getGenomeData().getParticipants().get("A").getRole()).isEqualTo("叙述者");
        assertThat(genome.getGenomeData().getParticipants().get("A").getAgeAtStory()).isEqualTo(25);

        assertThat(genome.getGenomeData().getKeyEvents()).hasSize(2);
        assertThat(genome.getGenomeData().getKeyEvents().get(0).getEvent()).isEqualTo("相识");

        assertThat(genome.getGenomeData().getCausalChain()).hasSize(2);
        assertThat(genome.getGenomeData().getConflictPatterns()).hasSize(1);
        assertThat(genome.getGenomeData().getLessons()).hasSize(1);
        assertThat(genome.getGenomeData().getEmotionalArc()).isNotNull();
        assertThat(genome.getGenomeData().getEmotionalArc().getTrajectory()).isEqualTo("decline");

        // 验证置信度
        assertThat(genome.getGenomeData().getConfidence()).isNotNull();
        assertThat(genome.getGenomeData().getConfidence().getOverall())
                .isEqualByComparingTo(new BigDecimal("0.85"));
    }

    // ====== 扁平化列同步 ======

    @Test
    void analyzeStory_fullGenome_syncsFlatColumns() {
        setupPromptMocks();
        when(llmCallService.call(anyString())).thenReturn(buildFullGenomeJson());

        RelationshipGenome genome = agent.analyzeStory(STORY_CONTENT, STORY_ID);

        // 扁平化列应与 genomeData 内部字段一致
        assertThat(genome.getRelationshipType()).isEqualTo("情侣");
        assertThat(genome.getOutcomeType()).isEqualTo("分手");
        assertThat(genome.getOverallConfidence())
                .isEqualByComparingTo(new BigDecimal("0.85"));
    }

    // ====== 部分 Genome（部分字段为 null） ======

    @Test
    void analyzeStory_partialGenome_handlesNullDimensions() {
        setupPromptMocks();
        // 只包含 relationship 和 confidence，其余维度缺失
        String partialJson = """
                {
                  "relationship": {
                    "type": "友谊",
                    "duration": "5年",
                    "stage": "持续",
                    "start_context": "职场"
                  },
                  "confidence": {
                    "overall": 0.45,
                    "relationship": 0.80,
                    "participants": 0.30,
                    "causal_chain": 0.35,
                    "conflict_patterns": 0.25
                  }
                }
                """;
        when(llmCallService.call(anyString())).thenReturn(partialJson);

        RelationshipGenome genome = agent.analyzeStory(STORY_CONTENT, STORY_ID);

        assertThat(genome).isNotNull();
        assertThat(genome.getGenomeData().getRelationship()).isNotNull();
        assertThat(genome.getGenomeData().getRelationship().getType()).isEqualTo("友谊");
        // 缺失维度为 null
        assertThat(genome.getGenomeData().getParticipants()).isNull();
        assertThat(genome.getGenomeData().getKeyEvents()).isNull();
        assertThat(genome.getGenomeData().getCausalChain()).isNull();
        assertThat(genome.getGenomeData().getConflictPatterns()).isNull();
        assertThat(genome.getGenomeData().getOutcome()).isNull();
        assertThat(genome.getGenomeData().getLessons()).isNull();
        assertThat(genome.getGenomeData().getEmotionalArc()).isNull();

        // 扁平化列同步 — outcome 为 null 时 outcomeType 应为 null
        assertThat(genome.getRelationshipType()).isEqualTo("友谊");
        assertThat(genome.getOutcomeType()).isNull();
        assertThat(genome.getOverallConfidence())
                .isEqualByComparingTo(new BigDecimal("0.45"));
    }

    // ====== 部分 Genome — relationship 为 null ======

    @Test
    void analyzeStory_nullRelationship_flatColumnIsNull() {
        setupPromptMocks();
        String json = """
                {
                  "confidence": {
                    "overall": 0.30
                  }
                }
                """;
        when(llmCallService.call(anyString())).thenReturn(json);

        RelationshipGenome genome = agent.analyzeStory(STORY_CONTENT, STORY_ID);

        assertThat(genome.getRelationshipType()).isNull();
        assertThat(genome.getOutcomeType()).isNull();
        assertThat(genome.getOverallConfidence())
                .isEqualByComparingTo(new BigDecimal("0.30"));
    }

    // ====== JSON 清洗 — markdown 代码块包裹 ======

    @Test
    void analyzeStory_markdownWrappedJson_cleansAndParses() {
        setupPromptMocks();
        String wrappedJson = "```json\n" + buildFullGenomeJson() + "\n```";
        when(llmCallService.call(anyString())).thenReturn(wrappedJson);

        RelationshipGenome genome = agent.analyzeStory(STORY_CONTENT, STORY_ID);

        assertThat(genome).isNotNull();
        assertThat(genome.getGenomeData().getRelationship().getType()).isEqualTo("情侣");
    }

    // ====== JSON 清洗 — 不带 json 关键字的代码块 ======

    @Test
    void analyzeStory_codeBlockWithoutJsonKeyword_cleansAndParses() {
        setupPromptMocks();
        String wrappedJson = "```\n" + buildFullGenomeJson() + "\n```";
        when(llmCallService.call(anyString())).thenReturn(wrappedJson);

        RelationshipGenome genome = agent.analyzeStory(STORY_CONTENT, STORY_ID);

        assertThat(genome).isNotNull();
        assertThat(genome.getGenomeData().getRelationship().getType()).isEqualTo("情侣");
    }

    // ====== JSON 清洗 — 前后有多余空白 ======

    @Test
    void analyzeStory_extraWhitespace_cleansCorrectly() {
        setupPromptMocks();
        String wrappedJson = "  \n ```json\n" + buildFullGenomeJson() + "\n``` \n ";
        when(llmCallService.call(anyString())).thenReturn(wrappedJson);

        RelationshipGenome genome = agent.analyzeStory(STORY_CONTENT, STORY_ID);

        assertThat(genome).isNotNull();
        assertThat(genome.getGenomeData().getRelationship().getType()).isEqualTo("情侣");
    }

    // ====== 空输入校验 — null storyContent ======

    @Test
    void analyzeStory_nullContent_throwsInvalidInput() {
        assertThatThrownBy(() -> agent.analyzeStory(null, STORY_ID))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> {
                    RkosException rkosEx = (RkosException) ex;
                    assertThat(rkosEx.getErrorCode()).isEqualTo("AGENT_INVALID_INPUT");
                    assertThat(rkosEx.getMessage()).contains("故事内容");
                });

        // 不应调用 LLM
        verifyNoInteractions(llmCallService);
    }

    // ====== 空输入校验 — 空白 storyContent ======

    @Test
    void analyzeStory_blankContent_throwsInvalidInput() {
        assertThatThrownBy(() -> agent.analyzeStory("   ", STORY_ID))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> assertThat(((RkosException) ex).getErrorCode())
                        .isEqualTo("AGENT_INVALID_INPUT"));

        verifyNoInteractions(llmCallService);
    }

    // ====== 空输入校验 — null storyId ======

    @Test
    void analyzeStory_nullStoryId_throwsInvalidInput() {
        assertThatThrownBy(() -> agent.analyzeStory(STORY_CONTENT, null))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> assertThat(((RkosException) ex).getErrorCode())
                        .isEqualTo("AGENT_INVALID_INPUT"));

        verifyNoInteractions(llmCallService);
    }

    // ====== 空输入校验 — 空白 storyId ======

    @Test
    void analyzeStory_blankStoryId_throwsInvalidInput() {
        assertThatThrownBy(() -> agent.analyzeStory(STORY_CONTENT, "  "))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> assertThat(((RkosException) ex).getErrorCode())
                        .isEqualTo("AGENT_INVALID_INPUT"));

        verifyNoInteractions(llmCallService);
    }

    // ====== 解析失败 — 非法 JSON ======

    @Test
    void analyzeStory_invalidJson_throwsParseFailed() {
        setupPromptMocks();
        when(llmCallService.call(anyString())).thenReturn("这不是 JSON 内容");

        assertThatThrownBy(() -> agent.analyzeStory(STORY_CONTENT, STORY_ID))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> {
                    RkosException rkosEx = (RkosException) ex;
                    assertThat(rkosEx.getErrorCode()).isEqualTo("AGENT_PARSE_FAILED");
                    assertThat(rkosEx.getMessage()).contains("解析失败");
                });
    }

    // ====== 解析失败 — 不完整 JSON ======

    @Test
    void analyzeStory_incompleteJson_throwsParseFailed() {
        setupPromptMocks();
        when(llmCallService.call(anyString())).thenReturn("{\"relationship\": {\"type\": \"情侣\"");

        assertThatThrownBy(() -> agent.analyzeStory(STORY_CONTENT, STORY_ID))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> assertThat(((RkosException) ex).getErrorCode())
                        .isEqualTo("AGENT_PARSE_FAILED"));
    }

    // ====== Prompt 加载与拼接验证 ======

    @Test
    void analyzeStory_verifiesPromptLoadingAndConcatenation() {
        setupPromptMocks();
        when(llmCallService.call(anyString())).thenReturn(buildMinimalGenomeJson());

        agent.analyzeStory(STORY_CONTENT, STORY_ID);

        // 验证 Prompt 加载
        verify(promptTemplateService).loadSystemPrompt("story-understanding");
        verify(promptTemplateService).loadUserTemplate("story-understanding");

        // 验证变量替换 — render 被调用，参数包含 story_content
        verify(promptTemplateService).render(eq(USER_TEMPLATE),
                argThat(vars -> STORY_CONTENT.equals(vars.get("story_content"))));

        // 验证 LLM 调用 — prompt 应为 system + "\n\n" + rendered userMessage 的精确拼接
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmCallService).call(promptCaptor.capture());
        String actualPrompt = promptCaptor.getValue();
        String renderedUserMessage = USER_TEMPLATE.replace("{story_content}", STORY_CONTENT);
        String expectedPrompt = SYSTEM_PROMPT + "\n\n" + renderedUserMessage;
        assertThat(actualPrompt).isEqualTo(expectedPrompt);
    }

    // ====== SNAKE_CASE → camelCase 映射验证 ======

    @Test
    void analyzeStory_snakeCaseJson_mapsToCamelCaseFields() {
        setupPromptMocks();
        // JSON 使用 snake_case（与 system-prompt.txt Schema 一致）
        String json = """
                {
                  "relationship": {
                    "type": "师生",
                    "start_context": "高中校园"
                  },
                  "participants": {
                    "A": {
                      "role": "学生",
                      "age_at_story": 17,
                      "attachment": "安全型",
                      "behaviors": ["认真听讲"],
                      "emotions": ["尊敬"],
                      "gender": "female"
                    }
                  },
                  "key_events": [
                    {
                      "event": "入学",
                      "position": "beginning",
                      "description": "第一次进入教室"
                    }
                  ],
                  "causal_chain": ["入学", "建立师生关系"],
                  "conflict_patterns": [],
                  "outcome": {
                    "type": "成长",
                    "initiator": "both",
                    "manner": "gradual"
                  },
                  "lessons": ["教育改变命运"],
                  "confidence": {
                    "overall": 0.75,
                    "relationship": 0.80,
                    "participants": 0.70,
                    "causal_chain": 0.72,
                    "conflict_patterns": 0.65
                  },
                  "emotional_arc": {
                    "dominant_emotions": ["尊敬", "感恩"],
                    "trajectory": "rise"
                  }
                }
                """;
        when(llmCallService.call(anyString())).thenReturn(json);

        RelationshipGenome genome = agent.analyzeStory(STORY_CONTENT, STORY_ID);

        // 验证 snake_case → camelCase 映射
        assertThat(genome.getGenomeData().getRelationship().getStartContext()).isEqualTo("高中校园");
        assertThat(genome.getGenomeData().getParticipants().get("A").getAgeAtStory()).isEqualTo(17);
        assertThat(genome.getGenomeData().getKeyEvents().get(0).getEvent()).isEqualTo("入学");
        assertThat(genome.getGenomeData().getEmotionalArc().getDominantEmotions())
                .containsExactly("尊敬", "感恩");
        assertThat(genome.getGenomeData().getConfidence().getCausalChain())
                .isEqualByComparingTo(new BigDecimal("0.72"));
        assertThat(genome.getGenomeData().getConfidence().getConflictPatterns())
                .isEqualByComparingTo(new BigDecimal("0.65"));
    }

    // ====== 未知字段容错（FAIL_ON_UNKNOWN_PROPERTIES = false） ======

    @Test
    void analyzeStory_unknownFields_ignoredGracefully() {
        setupPromptMocks();
        String jsonWithExtra = """
                {
                  "relationship": {
                    "type": "友谊",
                    "unknown_field": "should be ignored"
                  },
                  "extra_dimension": { "foo": "bar" },
                  "confidence": {
                    "overall": 0.60
                  }
                }
                """;
        when(llmCallService.call(anyString())).thenReturn(jsonWithExtra);

        RelationshipGenome genome = agent.analyzeStory(STORY_CONTENT, STORY_ID);

        assertThat(genome).isNotNull();
        assertThat(genome.getGenomeData().getRelationship().getType()).isEqualTo("友谊");
    }

    // ====== LLM 调用异常透传 ======

    @Test
    void analyzeStory_llmCallFails_propagatesException() {
        setupPromptMocks();
        when(llmCallService.call(anyString()))
                .thenThrow(new RkosException("LLM_CALL_FAILED", "LLM 调用失败（重试耗尽）"));

        assertThatThrownBy(() -> agent.analyzeStory(STORY_CONTENT, STORY_ID))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> assertThat(((RkosException) ex).getErrorCode())
                        .isEqualTo("LLM_CALL_FAILED"));
    }

    // ====== 辅助方法 ======

    /**
     * 设置 PromptTemplateService mock — 返回预设的 system prompt 和 user template。
     */
    private void setupPromptMocks() {
        when(promptTemplateService.loadSystemPrompt("story-understanding")).thenReturn(SYSTEM_PROMPT);
        when(promptTemplateService.loadUserTemplate("story-understanding")).thenReturn(USER_TEMPLATE);
        when(promptTemplateService.render(eq(USER_TEMPLATE), anyMap()))
                .thenAnswer(invocation -> {
                    String template = invocation.getArgument(0);
                    Map<String, String> vars = invocation.getArgument(1);
                    return template.replace("{story_content}", vars.getOrDefault("story_content", ""));
                });
    }

    /**
     * 构建完整 9 维度 Genome JSON（snake_case 格式，与 system-prompt.txt Schema 一致）。
     */
    private String buildFullGenomeJson() {
        return """
                {
                  "relationship": {
                    "type": "情侣",
                    "duration": "3年",
                    "stage": "冷淡期",
                    "start_context": "大学校园"
                  },
                  "participants": {
                    "A": {
                      "role": "叙述者",
                      "attachment": "焦虑型",
                      "behaviors": ["索取确认", "频繁追问"],
                      "emotions": ["焦虑", "不安"],
                      "age_at_story": 25,
                      "gender": "female"
                    },
                    "B": {
                      "role": "对方",
                      "attachment": "回避型",
                      "behaviors": ["回避沟通", "沉默应对"],
                      "emotions": ["疲惫", "无奈"],
                      "age_at_story": 27,
                      "gender": "male"
                    }
                  },
                  "key_events": [
                    {
                      "event": "相识",
                      "position": "beginning",
                      "description": "在大学图书馆偶然相遇"
                    },
                    {
                      "event": "矛盾爆发",
                      "position": "climax",
                      "description": "因沟通不畅产生严重分歧"
                    }
                  ],
                  "causal_chain": ["相识相知", "沟通减少导致误解加深"],
                  "conflict_patterns": [
                    {
                      "type": "communication",
                      "frequency": "recurring",
                      "resolution": "unresolved",
                      "description": "双方沟通方式差异导致反复冲突"
                    }
                  ],
                  "outcome": {
                    "type": "分手",
                    "initiator": "B",
                    "manner": "gradual"
                  },
                  "lessons": ["沟通是关系的基石"],
                  "confidence": {
                    "overall": 0.85,
                    "relationship": 0.90,
                    "participants": 0.82,
                    "causal_chain": 0.78,
                    "conflict_patterns": 0.80
                  },
                  "emotional_arc": {
                    "dominant_emotions": ["遗憾", "不舍"],
                    "trajectory": "decline"
                  }
                }
                """;
    }

    /**
     * 构建最小 Genome JSON（仅 confidence.overall）。
     */
    private String buildMinimalGenomeJson() {
        return """
                {
                  "confidence": {
                    "overall": 0.50
                  }
                }
                """;
    }
}
