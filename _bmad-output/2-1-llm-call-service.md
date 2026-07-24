# Story 2.1: LLM 调用封装服务

Status: done

## Story

作为**开发者**，
我希望有一个统一的 LLM 调用封装层，
以便所有 Agent 都通过同一接口调用大模型，并自动处理重试和提供商切换。

## Acceptance Criteria

1. [x] `LlmCallService.java` 已实现，位于 `com.rkos.common` 包下
2. [x] 通过 Spring AI 2.0.0 的 `ChatClient` 发送 LLM 请求
3. [x] 失败时自动重试最多 3 次（`@Retryable`，指数退避）
4. [x] 模型提供商可通过 `application.yml` 配置切换（NFR12）
5. [x] LLM 接口抽象为可插拔服务层（NFR15）
6. [x] 调用超时和异常有结构化日志记录
7. [x] 单元测试覆盖：成功调用、重试成功、重试耗尽、超时异常、配额异常

## Tasks / Subtasks

- [x] Task 1: 添加 `spring-retry` 依赖到 `pom.xml` (AC: #3)
  - [x] 1.1 添加 `spring-boot-starter-aspectj`（Spring Boot 4.x 中 `@Retryable` 底层依赖 AOP 代理，已从 `spring-boot-starter-aop` 改名）
  - [x] 1.2 单独添加 `spring-retry:2.0.6`（Spring Boot 4.1 BOM 未管理此版本）
- [x] Task 2: 启用 `@EnableRetry` (AC: #3)
  - [x] 2.1 在 `RetryConfig.java`（新建）上添加 `@EnableRetry`
- [x] Task 3: 实现 `LlmCallService` (AC: #1, #2, #4, #5, #6)
  - [x] 3.1 注入 `ChatClient.Builder`（由 Spring AI auto-config 提供），构建 `ChatClient` 实例
  - [x] 3.2 实现 `call(String prompt)` 方法，内部使用 `ChatClient` 调用 LLM
  - [x] 3.3 添加 `@Retryable` 注解：`maxAttempts = 3`，`backoff = @Backoff(delay = 1000, multiplier = 2)`
  - [x] 3.4 捕获并分类 LLM 异常（超时、配额不足、通用错误），转为 `RkosException`
  - [x] 3.5 结构化日志：记录耗时、响应长度、异常类型
- [x] Task 4: 配置层完善 (AC: #4)
  - [x] 4.1 确认 `application-dev.yml` 中 Spring AI 配置完整（api-key、base-url、model、connect-timeout、read-timeout）
  - [x] 4.2 在 `application-prod.yml` 中补充超时配置（connect-timeout: 10s, read-timeout: 120s）
- [x] Task 5: 单元测试 (AC: #7)
  - [x] 5.1 `LlmCallServiceTest.java`：Mock `ChatClient`，测试成功调用
  - [x] 5.2 测试重试机制：前 N-1 次失败后第 N 次成功
  - [x] 5.3 测试重试耗尽：3 次全部失败后抛出正确异常
  - [x] 5.4 测试异常分类：超时 → `LLM_CALL_FAILED`，配额 → `LLM_QUOTA_EXCEEDED`

## Dev Notes

### 前置验证情报（来自 Epic 1 回顾报告）

**Spring AI 2.0.0 兼容性验证结果（已通过）：**
- Spring AI auto-config Bean 创建正常：`openAiChatModel`、`chatClientBuilder`、`chatMemory` 等
- DashScope 端点已配置：`https://dashscope.aliyuncs.com/compatible-mode/v1`
- 模型配置：`qwen-max`（`application-dev.yml`）
- **关键注入方式：使用 `ChatClient.Builder`（而非直接注入 `ChatClient`）**
  - `ChatClient.Builder` 由 Spring AI auto-config 自动提供，通过 `.build()` 构建实例
- Spring AI 2.0.0 与 Spring Boot 4.1.0 完全兼容，无需额外适配

**Jackson 2.x 已就绪：**
- `Jackson2Config.java` 已手动注册 Jackson 2.x `ObjectMapper`
- Spring AI SDK（`openai-java-core:4.39.1`）硬依赖 Jackson 2.x，已通过此配置解决

### 架构约束（必须遵循）

1. **文件位置**：`LlmCallService.java` 放在 `com.rkos.common` 包下
   - [Source: architecture.md #项目结构模式] `common/` 包含 `LlmCallService.java`
2. **异常规范**：LLM 调用异常转换为 `RkosException`
   - 超时/通用错误 → `RkosException("LLM_CALL_FAILED", ...)`
   - 配额不足（HTTP 429）→ `RkosException("LLM_QUOTA_EXCEEDED", ...)`
3. **日志规范**：使用 `@Slf4j`，记录 model_used、耗时、retry_count
4. **禁止直接调用**：后续 Agent 必须通过 `LlmCallService` 调用 LLM，不直接注入 `ChatClient`
5. **配置外置**：模型提供商通过 `application-{profile}.yml` 配置，不硬编码

### 代码基线模式（Epic 1 建立的模式）

- **依赖注入**：`@RequiredArgsConstructor` + `private final` 字段（非 `@Autowired`）
- **异常类**：`RkosException(String errorCode, String message)` 和 `RkosException(String errorCode, String message, Throwable cause)`
- **全局异常处理**：`GlobalExceptionHandler` 已处理 `RkosException`，会自动转为 `ApiResponse`
- **命名规范**：类名 PascalCase，方法 camelCase，常量 UPPER_SNAKE_CASE

### 依赖添加注意事项

**`pom.xml` 当前状态：**
- `spring-ai-starter-model-openai` 已存在（Story 1-1 占位）
- **缺少 `spring-retry` 相关依赖**，需添加

**需添加的依赖：**
```xml
<!-- @Retryable 重试支持 -->
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

> `spring-retry` 版本由 Spring Boot BOM 管理，无需指定版本号。
> `spring-boot-starter-aop` 提供 AOP 代理支持，`@Retryable` 需要 AOP 拦截。

### `@EnableRetry` 配置

新建 `RetryConfig.java` 放在 `com.rkos.config` 包下：
```java
@Configuration
@EnableRetry
public class RetryConfig {
}
```

> 不要在 `RkosApplication.java` 上直接添加 `@EnableRetry`，遵循配置分离原则。

### `LlmCallService` 核心实现要点

1. **ChatClient 构建**：
```java
// 注入 ChatClient.Builder（Spring AI auto-config 自动提供）
private final ChatClient chatClient;

public LlmCallService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
}
```

> 由于使用 `@RequiredArgsConstructor`，ChatClient 不能直接用 final 字段注入（需要从 Builder 构建）。
> 推荐使用自定义构造函数或 `@PostConstruct`。

2. **@Retryable 方法签名**：
```java
@Retryable(
    retryFor = {Exception.class},
    noRetryFor = {RkosException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public String call(String prompt) { ... }
```

> Spring Retry 2.x（Spring Boot 4.x 内置）使用 `retryFor`/`noRetryFor` 替代了旧的 `value`/`exclude` 属性。

3. **超时控制**：通过 Spring AI 的配置项设置 LLM 调用超时，或在 `ChatClient` 调用时设置。

### 本 Story 不涉及的范围（Defer 到后续 Story）

- **Prompt 模板加载**（Story 2-2 `PromptTemplateService`）
- **Agent 编排**（Story 2-4 `StoryUnderstandingAgent`）
- **Genome 数据模型**（Story 2-3）
- **异步触发**（Story 2-6）
- **Prompt 热更新**（Story 4-3 Config Server）

### Project Structure Notes

新增文件：
```
src/main/java/com/rkos/
├── common/
│   └── LlmCallService.java          # 本 Story 新增
├── config/
│   └── RetryConfig.java              # 本 Story 新增
```

已有文件（不修改）：
```
├── common/
│   ├── ApiResponse.java
│   ├── GlobalExceptionHandler.java
│   └── RkosException.java
├── config/
│   ├── Jackson2Config.java
│   ├── SwaggerConfig.java
│   └── ApiKeyAuthConfig.java
```

修改文件：
```
├── pom.xml                            # 添加 spring-retry + spring-boot-starter-aop
├── src/main/resources/
│   └── application-dev.yml            # 确认 Spring AI 配置完整（可能无需修改）
│   └── application-prod.yml           # 补充 Spring AI 生产配置
```

### References

- [Source: epics.md #Story 2.1：LLM 调用封装服务]
- [Source: architecture.md #决策 3：Spring AI 原生 API]
- [Source: architecture.md #异常处理规范 #LLM 调用异常特殊处理]
- [Source: architecture.md #项目结构模式]
- [Source: epic-1-retro-2026-07-16.md #前置验证结果 #验证 2：Spring AI 2.0.0 兼容性预研]
- [Source: pom.xml #Spring AI 依赖]

### Review Findings

- [x] [Review][Decision] 重试耗尽后异常未映射为 LLM_CALL_FAILED（503），实际返回 500 — ✅ 已修复：添加 `@Recover` 方法将重试耗尽包装为 `RkosException("LLM_CALL_FAILED")`，由 GlobalExceptionHandler 映射为 503
- [x] [Review][Decision] 日志缺少 model_used 和 retry_count — ✅ 已修复 model_used（从 ChatResponse metadata 提取），retry_count defer 到后续 Story
- [x] [Review][Patch] prompt 参数未校验 null [LlmCallService.java:54] — ✅ 已修复：添加 `Objects.requireNonNull`
- [x] [Review][Patch] response chain 无 null 安全保护 [LlmCallService.java:63] — ✅ 已修复：添加 null 检查并抛出 RkosException
- [x] [Review][Patch] 未使用的 import RetryCallback [LlmCallServiceTest.java:12] — ✅ 已删除

## Dev Agent Record

### Agent Model Used

Qoder (dev-story)

### Debug Log References

- Spring Boot 4.1.0 将 `spring-boot-starter-aop` 改名为 `spring-boot-starter-aspectj`
- `spring-retry` 版本不由 Spring Boot 4.1 BOM 管理，需显式指定 2.0.6
- Mockito 无法抛出 checked exception（`SocketTimeoutException`），需使用 `RuntimeException` 包装

### Completion Notes List

- 新增 `LlmCallService.java`（`com.rkos.common`），通过 `ChatClient.Builder` 构建 `ChatClient`，统一 LLM 调用入口
- `@Retryable` 配置：maxAttempts=3，指数退避（1s → 2s → 4s），`RkosException` 不重试
- 异常分类：配额相关关键字（429/quota/rate limit/too many requests）→ `LLM_QUOTA_EXCEEDED`，其他 → `LLM_CALL_FAILED`
- 新增 `RetryConfig.java`（`com.rkos.config`），独立配置类启用 `@EnableRetry`
- `GlobalExceptionHandler` 补充 HTTP 状态码映射：`LLM_QUOTA_EXCEEDED` → 429，`LLM_CALL_FAILED` → 503
- `application-dev.yml` 和 `application-prod.yml` 补充 `connect-timeout: 10s` 和 `read-timeout: 120s`
- 11 个单元测试全部通过（成功调用、重试后成功、重试耗尽、超时分类、配额分类、rate limit 关键字、配额不重试、Builder 构建、null prompt、null response、null result）
- 全量 68 个测试通过，零回归
- **Code Review 修复：**
  - 添加 `Objects.requireNonNull(prompt)` 防御 null 输入
  - 添加 response chain null 安全检查（response/result/output/text）
  - 添加 `@Recover` 方法：重试耗尽后包装为 `RkosException("LLM_CALL_FAILED")` → 503
  - 补充 `model_used` 日志（从 `ChatResponse.getMetadata().getModel()` 提取）
  - 删除未使用的 `RetryCallback` import

### File List

**新增文件：**
- `src/main/java/com/rkos/common/LlmCallService.java`
- `src/main/java/com/rkos/config/RetryConfig.java`
- `src/test/java/com/rkos/common/LlmCallServiceTest.java`

**修改文件：**
- `pom.xml` — 添加 spring-retry:2.0.6 + spring-boot-starter-aspectj
- `src/main/java/com/rkos/common/GlobalExceptionHandler.java` — 补充 LLM 错误码 HTTP 状态映射
- `src/main/resources/application-dev.yml` — 补充 connect-timeout/read-timeout，更新注释
- `src/main/resources/application-prod.yml` — 补充 connect-timeout/read-timeout
