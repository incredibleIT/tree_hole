package com.rkos.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Prompt 模板加载服务。
 * <p>
 * 从外置文件加载 Prompt 模板（禁止硬编码），支持模板变量替换。
 * 模板文件存放于 {@code src/main/resources/prompts/} 目录下，按 Agent 名称分子目录组织。
 * <p>
 * 模板路径通过 {@code rkos.prompts.base-path} 配置项指定，默认值为 {@code classpath:/prompts/}。
 * 支持 {@code classpath:} 和 {@code file:} 两种协议前缀，由 Spring {@link ResourceLoader} 统一处理。
 *
 * @see LlmCallService
 * @see RkosException
 */
@Service
@Slf4j
public class PromptTemplateService {

    private final String basePath;
    private final ResourceLoader resourceLoader;

    /**
     * 构造函数注入。
     *
     * @param basePath         Prompt 模板根目录（含协议前缀，如 classpath:/prompts/）
     * @param resourceLoader   Spring 资源加载器（自动注入）
     */
    public PromptTemplateService(
            @Value("${rkos.prompts.base-path:classpath:/prompts/}") String basePath,
            ResourceLoader resourceLoader) {
        this.basePath = basePath.endsWith("/") ? basePath : basePath + "/";
        this.resourceLoader = resourceLoader;
    }

    /**
     * 加载指定 Agent 的系统提示词。
     *
     * @param agentName Agent 名称（如 story-understanding）
     * @return 系统提示词内容
     * @throws RkosException 当模板文件不存在或读取失败时抛出
     */
    public String loadSystemPrompt(String agentName) {
        Objects.requireNonNull(agentName, "agentName 不能为 null");
        validateAgentName(agentName);
        return loadTemplate(agentName + "/system-prompt.txt");
    }

    /**
     * 加载指定 Agent 的用户模板（含变量占位符）。
     *
     * @param agentName Agent 名称（如 story-understanding）
     * @return 用户模板内容
     * @throws RkosException 当模板文件不存在或读取失败时抛出
     */
    public String loadUserTemplate(String agentName) {
        Objects.requireNonNull(agentName, "agentName 不能为 null");
        validateAgentName(agentName);
        return loadTemplate(agentName + "/user-template.txt");
    }

    /**
     * 对模板字符串执行变量替换。
     * <p>
     * 变量占位符格式为 {@code {variable_name}}（单大括号）。
     * 如果变量值为 null，替换为空字符串。
     *
     * @param template  模板字符串（含 {@code {key}} 占位符）
     * @param variables 变量键值对
     * @return 替换后的字符串
     */
    public String render(String template, Map<String, String> variables) {
        Objects.requireNonNull(template, "template 不能为 null");
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        StringBuilder result = new StringBuilder(template.length());
        int i = 0;
        while (i < template.length()) {
            if (template.charAt(i) == '{') {
                int close = template.indexOf('}', i + 1);
                if (close > i + 1) {
                    String key = template.substring(i + 1, close);
                    if (variables.containsKey(key)) {
                        String value = variables.get(key);
                        result.append(value != null ? value : "");
                        i = close + 1;
                        continue;
                    }
                }
            }
            result.append(template.charAt(i));
            i++;
        }
        return result.toString();
    }

    /**
     * 从 basePath 下加载指定相对路径的模板文件。
     *
     * @param relativePath 相对于 basePath 的路径（如 story-understanding/system-prompt.txt）
     * @return 模板文件内容（UTF-8）
     * @throws RkosException 当文件不存在或读取失败时抛出
     */
    private void validateAgentName(String agentName) {
        if (agentName.contains("..") || agentName.contains("/") || agentName.contains("\\")) {
            throw new RkosException("PROMPT_LOAD_ERROR", "非法的 Agent 名称: " + agentName);
        }
    }

    private String loadTemplate(String relativePath) {
        String fullPath = basePath + relativePath;
        log.debug("加载 Prompt 模板: {}", fullPath);

        try {
            Resource resource = resourceLoader.getResource(fullPath);
            if (!resource.exists()) {
                throw new RkosException("PROMPT_LOAD_ERROR", "Prompt 模板不存在: " + fullPath);
            }
            try (InputStream is = resource.getInputStream()) {
                String content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
                log.debug("Prompt 模板加载成功: {}, 长度: {}", fullPath, content.length());
                return content;
            }
        } catch (RkosException e) {
            throw e;
        } catch (IOException e) {
            throw new RkosException("PROMPT_LOAD_ERROR", "Prompt 模板读取失败: " + fullPath, e);
        }
    }
}
