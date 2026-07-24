package com.rkos.modules.story.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rkos.common.LlmCallService;
import com.rkos.common.PromptTemplateService;
import com.rkos.common.RkosException;
import com.rkos.modules.story.model.GenomeData;
import com.rkos.modules.story.model.RelationshipGenome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 故事理解 Agent，从故事文本中自动抽取结构化关系特征，生成标准化的关系基因组。
 * <p>
 * 通过 {@link LlmCallService} 统一调用 LLM（禁止直接注入 ChatClient），
 * 通过 {@link PromptTemplateService} 加载外置 Prompt 模板（禁止硬编码）。
 * <p>
 * LLM 返回的 JSON 经 Jackson 反序列化为 {@link GenomeData}，再构建 {@link RelationshipGenome}
 * （含扁平化列同步：relationshipType、outcomeType、overallConfidence）。
 */
@Service
@Slf4j
public class StoryUnderstandingAgent {

    private static final String AGENT_NAME = "story-understanding";
    private static final String AGENT_VERSION = "v1.0";

    private final LlmCallService llmCallService;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入。
     * <p>
     * 自建 {@link ObjectMapper}（不使用 Spring 注入的全局 bean），
     * 因为 Agent 需要 {@code SNAKE_CASE} 命名策略来映射 JSON Schema（snake_case）→ Java 模型（camelCase）。
     *
     * @param llmCallService      LLM 调用封装服务
     * @param promptTemplateService Prompt 模板加载服务
     */
    public StoryUnderstandingAgent(LlmCallService llmCallService,
                                   PromptTemplateService promptTemplateService) {
        this.llmCallService = llmCallService;
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // JSON Schema 使用 snake_case（如 start_context），Java 模型使用 camelCase（如 startContext）
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * 分析故事内容，抽取关系基因组。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>输入校验</li>
     *   <li>加载 system-prompt + user-template</li>
     *   <li>变量替换：{story_content} → storyContent</li>
     *   <li>拼接完整 prompt（system + "\n\n" + userMessage）</li>
     *   <li>调用 LlmCallService.call(fullPrompt)</li>
     *   <li>清洗 LLM 响应（去除 markdown 代码块标记）</li>
     *   <li>Jackson 反序列化为 GenomeData</li>
     *   <li>构建 RelationshipGenome（含扁平化列同步）</li>
     * </ol>
     *
     * @param storyContent 故事文本内容
     * @param storyId      故事 ID（UUID，对应 MongoDB Story._id）
     * @return 完整的关系基因组对象
     * @throws RkosException 当输入无效或 LLM 响应解析失败时抛出
     */
    public RelationshipGenome analyzeStory(String storyContent, String storyId) {
        // 1. 输入校验
        validateInput(storyContent, storyId);

        log.info("开始故事理解分析, storyId: {}, 内容长度: {}", storyId, storyContent.length());

        // 2. 加载 Prompt 模板
        String systemPrompt = promptTemplateService.loadSystemPrompt(AGENT_NAME);
        String userTemplate = promptTemplateService.loadUserTemplate(AGENT_NAME);

        // 3. 变量替换
        String userMessage = promptTemplateService.render(userTemplate,
                Map.of("story_content", storyContent));

        // 4. 拼接完整 prompt（system prompt 作为指令前缀）
        String fullPrompt = systemPrompt + "\n\n" + userMessage;
        log.debug("完整 Prompt 长度: {}", fullPrompt.length());

        // 5. 调用 LLM
        String rawResponse = llmCallService.call(fullPrompt);
        log.debug("LLM 原始响应长度: {}", rawResponse.length());

        // 6. 清洗响应（去除 markdown 代码块标记）
        String cleanJson = cleanJsonResponse(rawResponse);

        // 7. 反序列化为 GenomeData
        GenomeData genomeData = parseGenomeData(cleanJson, storyId);

        // 8. 构建 RelationshipGenome（含扁平化列同步）
        RelationshipGenome genome = buildGenome(genomeData, storyId);

        log.info("故事理解分析完成, storyId: {}, 置信度: {}",
                storyId, genome.getOverallConfidence());

        return genome;
    }

    /**
     * 校验输入参数。
     *
     * @param storyContent 故事内容
     * @param storyId      故事 ID
     * @throws RkosException 当输入为 null 或空白时抛出
     */
    private void validateInput(String storyContent, String storyId) {
        if (storyContent == null || storyContent.isBlank()) {
            throw new RkosException("AGENT_INVALID_INPUT", "故事内容不能为空或空白");
        }
        if (storyId == null || storyId.isBlank()) {
            throw new RkosException("AGENT_INVALID_INPUT", "故事 ID 不能为空或空白");
        }
    }

    /**
     * 清洗 LLM 返回的 JSON 字符串，去除 markdown 代码块包裹。
     * <p>
     * LLM 可能将 JSON 包裹在 {@code ```json ... ```} 中，需要提取其中的纯 JSON。
     *
     * @param rawResponse LLM 原始响应
     * @return 清洗后的 JSON 字符串
     */
    private String cleanJsonResponse(String rawResponse) {
        String trimmed = rawResponse.trim();
        // 去除 ```json ... ``` 或 ``` ... ``` 包裹
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastBacktick = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastBacktick > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastBacktick).trim();
            }
        }
        return trimmed;
    }

    /**
     * 将 JSON 字符串反序列化为 {@link GenomeData} 对象。
     *
     * @param cleanJson 清洗后的 JSON 字符串
     * @param storyId   故事 ID（用于日志）
     * @return GenomeData 对象
     * @throws RkosException 当 JSON 解析失败时抛出
     */
    private GenomeData parseGenomeData(String cleanJson, String storyId) {
        try {
            GenomeData genomeData = objectMapper.readValue(cleanJson, GenomeData.class);
            log.debug("GenomeData 反序列化成功, storyId: {}", storyId);
            return genomeData;
        } catch (JsonProcessingException e) {
            log.error("GenomeData 反序列化失败, storyId: {}, 错误: {}, JSON 前100字符: {}",
                    storyId, e.getMessage(),
                    cleanJson.length() > 100 ? cleanJson.substring(0, 100) + "..." : cleanJson);
            throw new RkosException("AGENT_PARSE_FAILED",
                    "LLM 响应解析失败: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * 构建 {@link RelationshipGenome} 对象，同步扁平化列。
     * <p>
     * 扁平化列（relationshipType、outcomeType、overallConfidence）
     * 与 genomeData 内部字段保持一致，用于高频查询。
     *
     * @param genomeData 解析后的基因组数据
     * @param storyId    故事 ID
     * @return 完整的 RelationshipGenome 对象
     */
    private RelationshipGenome buildGenome(GenomeData genomeData, String storyId) {
        return RelationshipGenome.builder()
                .storyId(storyId)
                .agentVersion(AGENT_VERSION)
                .genomeData(genomeData)
                // 扁平化列同步
                .relationshipType(genomeData.getRelationship() != null
                        ? genomeData.getRelationship().getType() : null)
                .outcomeType(genomeData.getOutcome() != null
                        ? genomeData.getOutcome().getType() : null)
                .overallConfidence(genomeData.getConfidence() != null
                        ? genomeData.getConfidence().getOverall() : null)
                .build();
    }
}
