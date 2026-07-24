package com.rkos.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PromptTemplateService} 单元测试。
 * <p>
 * 使用真实 classpath 资源（{@code src/test/resources/prompts/test-agent/}）验证模板加载，
 * 不依赖 Spring 应用上下文。
 */
class PromptTemplateServiceTest {

    private PromptTemplateService service;

    @BeforeEach
    void setUp() {
        // 使用测试 classpath 下的 prompts 目录
        service = new PromptTemplateService(
                "classpath:/prompts/",
                new DefaultResourceLoader()
        );
    }

    // ====== 加载系统提示词 ======

    @Test
    void loadSystemPrompt_success() {
        String content = service.loadSystemPrompt("test-agent");

        assertThat(content).isNotBlank();
        assertThat(content).contains("测试 Agent");
    }

    // ====== 加载用户模板 ======

    @Test
    void loadUserTemplate_success() {
        String content = service.loadUserTemplate("test-agent");

        assertThat(content).isNotBlank();
        assertThat(content).contains("{story_content}");
        assertThat(content).contains("{relationship_type}");
    }

    // ====== 变量替换 ======

    @Test
    void render_replacesVariables() {
        String template = "请分析：{story_content}，类型：{relationship_type}";
        Map<String, String> variables = Map.of(
                "story_content", "我和她的故事",
                "relationship_type", "情侣"
        );

        String result = service.render(template, variables);

        assertThat(result).isEqualTo("请分析：我和她的故事，类型：情侣");
    }

    @Test
    void render_nullVariables_returnsOriginal() {
        String template = "原始内容 {key}";

        String result = service.render(template, null);

        assertThat(result).isEqualTo("原始内容 {key}");
    }

    @Test
    void render_emptyVariables_returnsOriginal() {
        String template = "原始内容 {key}";

        String result = service.render(template, Map.of());

        assertThat(result).isEqualTo("原始内容 {key}");
    }

    @Test
    void render_nullValue_replacedWithEmpty() {
        String template = "内容：{key}";
        Map<String, String> variables = new HashMap<>();
        variables.put("key", null);

        String result = service.render(template, variables);

        assertThat(result).isEqualTo("内容：");
    }

    @Test
    void render_partialMatch_onlyReplacesMatchingKeys() {
        String template = "{a} 和 {b} 和 {c}";
        Map<String, String> variables = Map.of("a", "甲", "c", "丙");

        String result = service.render(template, variables);

        assertThat(result).isEqualTo("甲 和 {b} 和 丙");
    }

    // ====== 模板不存在异常 ======

    @Test
    void loadSystemPrompt_notFound_throwsRkosException() {
        assertThatThrownBy(() -> service.loadSystemPrompt("non-existent-agent"))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> {
                    RkosException rkosEx = (RkosException) ex;
                    assertThat(rkosEx.getErrorCode()).isEqualTo("PROMPT_LOAD_ERROR");
                    assertThat(rkosEx.getMessage()).contains("non-existent-agent");
                });
    }

    @Test
    void loadUserTemplate_notFound_throwsRkosException() {
        assertThatThrownBy(() -> service.loadUserTemplate("non-existent-agent"))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> {
                    RkosException rkosEx = (RkosException) ex;
                    assertThat(rkosEx.getErrorCode()).isEqualTo("PROMPT_LOAD_ERROR");
                    assertThat(rkosEx.getMessage()).contains("non-existent-agent");
                });
    }

    // ====== null 参数防御 ======

    @Test
    void loadSystemPrompt_nullAgentName_throwsNullPointerException() {
        assertThatThrownBy(() -> service.loadSystemPrompt(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("agentName");
    }

    @Test
    void loadUserTemplate_nullAgentName_throwsNullPointerException() {
        assertThatThrownBy(() -> service.loadUserTemplate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("agentName");
    }

    @Test
    void render_nullTemplate_throwsNullPointerException() {
        assertThatThrownBy(() -> service.render(null, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("template");
    }

    // ====== 加载 story-understanding 实际模板（集成验证） ======

    @Test
    void loadSystemPrompt_storyUnderstanding_loadsRealTemplate() {
        // story-understanding 模板在 src/main/resources/prompts/ 下，也在 classpath 中
        String content = service.loadSystemPrompt("story-understanding");

        assertThat(content).isNotBlank();
        assertThat(content).contains("关系知识抽取专家");
        assertThat(content).contains("Relationship Genome");
    }

    @Test
    void loadUserTemplate_storyUnderstanding_containsVariablePlaceholder() {
        String content = service.loadUserTemplate("story-understanding");

        assertThat(content).isNotBlank();
        assertThat(content).contains("{story_content}");
    }

    // ====== Code Review 修复验证 ======

    @Test
    void loadSystemPrompt_pathTraversal_dotDot_throwsRkosException() {
        assertThatThrownBy(() -> service.loadSystemPrompt("../etc"))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> {
                    RkosException rkosEx = (RkosException) ex;
                    assertThat(rkosEx.getErrorCode()).isEqualTo("PROMPT_LOAD_ERROR");
                    assertThat(rkosEx.getMessage()).contains("\u975e\u6cd5");
                });
    }

    @Test
    void loadSystemPrompt_pathTraversal_slash_throwsRkosException() {
        assertThatThrownBy(() -> service.loadSystemPrompt("foo/bar"))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> {
                    RkosException rkosEx = (RkosException) ex;
                    assertThat(rkosEx.getErrorCode()).isEqualTo("PROMPT_LOAD_ERROR");
                });
    }

    @Test
    void loadSystemPrompt_pathTraversal_backslash_throwsRkosException() {
        assertThatThrownBy(() -> service.loadSystemPrompt("foo\\bar"))
                .isInstanceOf(RkosException.class)
                .satisfies(ex -> {
                    RkosException rkosEx = (RkosException) ex;
                    assertThat(rkosEx.getErrorCode()).isEqualTo("PROMPT_LOAD_ERROR");
                });
    }

    @Test
    void constructor_basePathWithoutTrailingSlash_normalizesAutomatically() {
        PromptTemplateService svc = new PromptTemplateService(
                "classpath:/prompts",  // \u65e0\u5c3e\u90e8\u659c\u6760
                new DefaultResourceLoader()
        );
        // \u5e94\u80fd\u6b63\u5e38\u52a0\u8f7d\u6a21\u677f
        String content = svc.loadSystemPrompt("test-agent");
        assertThat(content).isNotBlank();
        assertThat(content).contains("\u6d4b\u8bd5 Agent");
    }

    @Test
    void render_variableValueContainsPlaceholder_noCascadeReplacement() {
        String template = "{name}，{greeting}";
        Map<String, String> variables = Map.of(
                "name", "{greeting}\u5148\u751f",
                "greeting", "\u65e9\u4e0a\u597d"
        );

        String result = service.render(template, variables);

        // name \u7684\u503c\u4e2d\u7684 {greeting} \u4e0d\u5e94\u88ab\u7ea7\u8054\u66ff\u6362
        assertThat(result).isEqualTo("{greeting}\u5148\u751f\uff0c\u65e9\u4e0a\u597d");
    }
}
