# Story 2.2: Prompt 模板加载服务

Status: done

## Story

作为**开发者**，
我希望 Prompt 模板从外置文件加载而非硬编码，
以便在不修改代码的情况下调整 Agent 行为。

## Acceptance Criteria

1. [x] `PromptTemplateService.java` 已实现，位于 `com.rkos.common` 包下
2. [x] 从 `src/main/resources/prompts/` 目录读取对应的 `.txt` 文件
3. [x] 支持模板变量替换（如 `{story_content}`、`{relationship_type}`）
4. [x] 模板文件不存在时抛出明确异常（`RkosException("PROMPT_LOAD_ERROR", ...)`）
5. [x] `story-understanding/system-prompt.txt` 和 `user-template.txt` 已创建
6. [x] 单元测试覆盖：成功加载、变量替换、模板不存在异常

## Tasks / Subtasks

- [x] Task 1: 实现 `PromptTemplateService` (AC: #1, #2, #3, #4)
  - [x] 1.1 在 `com.rkos.common` 包下创建 `PromptTemplateService.java`
  - [x] 1.2 实现 `loadSystemPrompt(String agentName)` — 加载 `{agentName}/system-prompt.txt`
  - [x] 1.3 实现 `loadUserTemplate(String agentName)` — 加载 `{agentName}/user-template.txt`
  - [x] 1.4 实现 `render(String template, Map<String, String> variables)` — 变量替换
  - [x] 1.5 模板文件不存在时抛出 `RkosException("PROMPT_LOAD_ERROR", ...)`
- [x] Task 2: 创建 Prompt 模板文件 (AC: #5)
  - [x] 2.1 创建 `src/main/resources/prompts/story-understanding/system-prompt.txt`
  - [x] 2.2 创建 `src/main/resources/prompts/story-understanding/user-template.txt`
- [x] Task 3: 配置层 (AC: #2)
  - [x] 3.1 在 `application.yml` 中添加 `rkos.prompts.base-path` 配置项
- [x] Task 4: 单元测试 (AC: #6)
  - [x] 4.1 创建 `PromptTemplateServiceTest.java`
  - [x] 4.2 测试成功加载系统提示词
  - [x] 4.3 测试成功加载用户模板
  - [x] 4.4 测试变量替换功能
  - [x] 4.5 测试模板文件不存在时抛出 `RkosException`
  - [x] 4.6 在 `src/test/resources/prompts/` 下创建测试专用模板

## Dev Notes

### 前置 Story 情报（Story 2-1）

**已完成的基础设施：**
- `LlmCallService.java`（`com.rkos.common`）已实现 — 本 Story 的 `PromptTemplateService` 与之同包
- `RkosException.java`（`com.rkos.common`）已实现 — 直接使用其 `(String errorCode, String message)` 和 `(String errorCode, String message, Throwable cause)` 构造器
- `GlobalExceptionHandler` 已处理 `RkosException` — `PROMPT_LOAD_ERROR` 会被映射为 HTTP 500（默认）
- Story 2-1 的 68 个测试全部通过，dev 分支代码未 commit

**代码模式基线（Epic 1 建立）：**
- **依赖注入**：构造函数注入，`private final` 字段（不用 `@Autowired` 注解）
- **异常类**：`RkosException(String errorCode, String message)` 和 `RkosException(String errorCode, String message, Throwable cause)`
- **日志**：使用 `@Slf4j`（Lombok）
- **命名**：类名 PascalCase，方法 camelCase，常量 UPPER_SNAKE_CASE

### 架构约束（必须遵循）

1. **文件位置**：`PromptTemplateService.java` 放在 `com.rkos.common` 包下
   - [Source: architecture.md #项目结构模式] `common/` 包含 `PromptTemplateService.java`
2. **Prompt 外置管理**：所有 Prompt 模板放在 `src/main/resources/prompts/`，**禁止硬编码**
   - [Source: architecture.md #强制一致性规则 第3条]
3. **异常规范**：模板加载失败抛出 `RkosException("PROMPT_LOAD_ERROR", ...)`
   - [Source: architecture.md #Agent 调用与 Prompt 管理规范]
4. **配置路径**：通过 `rkos.prompts.base-path` 配置项指定模板根目录
   - [Source: architecture.md #决策 5：Prompt 热更新]

### 核心实现要点

**1. 类结构设计：**

```java
@Service
@Slf4j
public class PromptTemplateService {

    private final String basePath;

    public PromptTemplateService(
            @Value("${rkos.prompts.base-path:classpath:/prompts/}") String basePath) {
        this.basePath = basePath;
    }

    public String loadSystemPrompt(String agentName) { ... }
    public String loadUserTemplate(String agentName) { ... }
    public String render(String template, Map<String, String> variables) { ... }
}
```

> **关键**：构造函数注入 `@Value`（不用字段 `@Autowired`），与 `LlmCallService` 模式一致。
> `basePath` 默认值为 `classpath:/prompts/`，保持向后兼容。

**2. 模板加载逻辑：**

```java
// 路径拼接：basePath + agentName + "/system-prompt.txt"
// basePath 格式为 "classpath:/prompts/" 或 "file:/path/to/prompts/"
// 使用 ClassPathResource 或 ResourceLoader 读取
// 读取失败时 throw new RkosException("PROMPT_LOAD_ERROR", "无法加载 Prompt: " + path, e)
```

> **注意**：`basePath` 含 `classpath:` 前缀时需正确处理。推荐使用 Spring 的 `ResourceLoader` 统一处理 classpath/file 协议，而非手动解析。

**3. 变量替换逻辑：**

```java
public String render(String template, Map<String, String> variables) {
    String result = template;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
        result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return result;
}
```

> 使用简单 `String.replace("{key}", value)` 方式，不引入额外模板引擎依赖。
> 变量占位符格式为 `{variable_name}`（单大括号）。
> 如果变量值为 null，替换为空字符串。

**4. Prompt 模板文件内容指引：**

`system-prompt.txt`（系统提示词）— 定义 Agent 角色和输出格式：
```
你是一个关系知识抽取专家。你的任务是从用户提交的故事中抽取关键的关系信息，
并生成标准化的 Relationship Genome。

请严格按照 JSON 格式输出，包含以下维度：
- relationship（关系类型、持续时间、阶段、起始背景）
- participants（参与者A和B的角色、依恋类型、行为、情绪、年龄、性别）
- key_events（关键事件列表）
- causal_chain（因果链）
- conflict_patterns（冲突模式）
- outcome（结果类型、发起者、方式）
- lessons（领悟）
- confidence（各维度置信度 0.00-1.00）
- emotional_arc（情感弧线）
```

`user-template.txt`（用户模板）— 含变量占位符：
```
请分析以下故事，抽取关系基因组信息：

{story_content}

请严格按照系统提示词中定义的 JSON 格式输出。
```

> 模板内容应为中文（与系统整体语言一致），且足够详细以引导 LLM 输出正确格式。
> 这只是初始版本，后续 Story 2-4（StoryUnderstandingAgent）会进一步完善 Prompt 内容。

### 配置变更

**`application.yml` 需添加：**
```yaml
rkos:
  prompts:
    base-path: classpath:/prompts/
```

> 添加到已有的 `rkos:` 配置块下（与 `rkos.api` 同级）。
> 当前 `application.yml` 已有 `rkos.api.enabled: true`，在其下方追加即可。

### 本 Story 不涉及的范围（Defer 到后续 Story）

- **Agent 调用编排**（Story 2-4 `StoryUnderstandingAgent`）— 本 Story 只提供加载能力
- **Prompt 内容精调**（Story 2-4 实现时迭代优化）
- **Config Server 热更新**（Story 4-3）— 本 Story 仅支持 classpath 加载
- **Prompt 缓存**（性能优化，后续评估）
- **BeanOutputConverter 结构化输出**（Story 2-4）

### Project Structure Notes

新增文件：
```
src/main/java/com/rkos/
├── common/
│   └── PromptTemplateService.java       # 本 Story 新增

src/main/resources/
├── prompts/
│   └── story-understanding/
│       ├── system-prompt.txt            # 本 Story 新增
│       └── user-template.txt            # 本 Story 新增

src/test/java/com/rkos/
├── common/
│   └── PromptTemplateServiceTest.java   # 本 Story 新增

src/test/resources/
└── prompts/
    └── test-agent/
        ├── system-prompt.txt            # 测试专用模板
        └── user-template.txt            # 测试专用模板
```

已有文件（不修改）：
```
├── common/
│   ├── ApiResponse.java
│   ├── GlobalExceptionHandler.java
│   ├── LlmCallService.java
│   └── RkosException.java
├── config/
│   ├── RetryConfig.java
│   ├── Jackson2Config.java
│   ├── SwaggerConfig.java
│   └── ApiKeyAuthConfig.java
```

修改文件：
```
├── src/main/resources/
│   └── application.yml                  # 添加 rkos.prompts.base-path 配置
```

### References

- [Source: epics.md #Story 2.2：Prompt 模板加载服务]
- [Source: architecture.md #决策 5：Prompt 热更新（Spring Cloud Config）]
- [Source: architecture.md #Agent 调用与 Prompt 管理规范]
- [Source: architecture.md #项目结构模式 — common/ 目录]
- [Source: architecture.md #强制一致性规则 — 第3条 Prompt 管理一致性]
- [Source: architecture.md #正反示例对比 — Prompt 模板文件示例]
- [Source: 2-1-llm-call-service.md #Story 2-1 完成情报]

### Review Findings

- [x] [Review][Patch] agentName 路径遍历漏洞 — 缺少 `..`/`/`/`\` 校验，攻击者可逃逸 prompts 目录 [PromptTemplateService.java:55-68] ✅ 已修复
- [x] [Review][Patch] basePath 缺少尾部斜杠校验 — 配置遗漏 `/` 时路径拼接错误 [PromptTemplateService.java:41-46] ✅ 已修复
- [x] [Review][Patch] render() 变量值含占位符时级联替换 — HashMap 遍历顺序不确定导致结果不可预期 [PromptTemplateService.java:82-93] ✅ 已修复
- [x] [Review][Defer] 模板文件无大小限制 — 超大模板文件一次性读入内存可能 OOM [PromptTemplateService.java:106-115] — deferred, pre-existing（实际模板文件远小于内存，后续评估）

## Dev Agent Record

### Agent Model Used

Claude Sonnet 4 (2025-05-14)

### Debug Log References

- 全量测试 82 个通过（含 PromptTemplateServiceTest 14 个），BUILD SUCCESS
- 无编译警告、无测试失败

### Completion Notes List

- PromptTemplateService 使用 `ResourceLoader` 统一处理 classpath/file 协议，比规格文件中的 `ClassPathResource` 方案更灵活
- 额外增加了 null 参数防御测试（3 个）和实际模板集成验证测试（2 个）
- 测试总计 14 个：加载成功 2 + 变量替换 5 + 异常 2 + null 防御 3 + 集成验证 2

### File List

**新增文件：**
- `src/main/java/com/rkos/common/PromptTemplateService.java` — Prompt 模板加载服务（123 行）
- `src/test/java/com/rkos/common/PromptTemplateServiceTest.java` — 单元测试（174 行，14 个测试）
- `src/test/resources/prompts/test-agent/system-prompt.txt` — 测试用系统提示词
- `src/test/resources/prompts/test-agent/user-template.txt` — 测试用用户模板

**已有文件（Story 2-2 创建，非本次 dev 新增）：**
- `src/main/resources/prompts/story-understanding/system-prompt.txt` — 关系知识抽取系统提示词
- `src/main/resources/prompts/story-understanding/user-template.txt` — 用户模板

**修改文件：**
- `src/main/resources/application.yml` — 添加 `rkos.prompts.base-path: classpath:/prompts/`
