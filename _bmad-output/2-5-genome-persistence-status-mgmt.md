# Story 2.5: Genome 持久化与处理状态管理

Status: done

## Story

作为**系统维护者**，
我希望 Agent 生成的 Genome 自动持久化到 PostgreSQL，同时更新 MongoDB 中的处理状态，
以便追踪每个故事的处理进度。

## Acceptance Criteria

1. [x] `StoryPersistenceService.java` 已实现，位于 `com.rkos.modules.story.service` 包下
2. [x] Agent 处理完成后，Genome 数据写入 PostgreSQL `relationship_genomes` 表（通过 `GenomeMapper`）
3. [x] MongoDB 故事的 `processing_status` 更新为 `COMPLETED`（成功时）或 `FAILED`（失败时）
4. [x] `processing_metadata` 记录 `agent_version`、`model_used`、`started_at`、`completed_at`
5. [x] 处理失败时 `processing_status` 更新为 `FAILED`，`error_message` 记录原因
6. [x] MongoDB 和 PostgreSQL 的写入通过应用层协调，失败走补偿逻辑
7. [x] 持久化前补全 `GenomeData.storyId` / `genomeId` 字段（解决 Story 2-4 审查遗留）
8. [x] 单元测试覆盖：正常流程、PostgreSQL 写入失败、MongoDB 更新失败、空 Genome 输入

## Tasks / Subtasks

- [x] Task 1: 实现 StoryPersistenceService 核心逻辑 (AC: #1, #2, #3, #4, #5, #6, #7)
  - [x] 1.1 在 `com.rkos.modules.story.service` 包下创建 `StoryPersistenceService.java`
  - [x] 1.2 构造函数注入 `GenomeMapper`、`StoryMongoRepository`
  - [x] 1.3 实现 `persistGenome(String storyId, RelationshipGenome genome)` 方法
  - [x] 1.4 补全 GenomeData 内部字段：设置 `storyId`（如果为 null）
  - [x] 1.5 先更新 MongoDB `processing_status` → `PROCESSING`，记录 `started_at`
  - [x] 1.6 写入 PostgreSQL `relationship_genomes` 表（`GenomeMapper.insert()`）
  - [x] 1.7 成功后更新 MongoDB `processing_status` → `COMPLETED`，记录 `completed_at`、`agent_version`、`model_used`
  - [x] 1.8 失败时更新 MongoDB `processing_status` → `FAILED`，记录 `error_message`
- [x] Task 2: 补偿与容错 (AC: #5, #6)
  - [x] 2.1 PostgreSQL 写入失败：捕获异常，更新 MongoDB 为 FAILED，记录 error_message
  - [x] 2.2 MongoDB 更新失败（PROCESSING 状态后）：记录 ERROR 日志，PostgreSQL 数据保留（补偿机制）
  - [x] 2.3 输入校验：storyId 为 null/空白、genome 为 null 时抛出 `RkosException`
- [x] Task 3: 单元测试 (AC: #8)
  - [x] 3.1 创建 `StoryPersistenceServiceTest.java`（Mockito mock `GenomeMapper`、`StoryMongoRepository`）
  - [x] 3.2 测试用例：正常持久化流程、PostgreSQL 写入失败降级、MongoDB 更新失败日志、空输入校验、GenomeData.storyId 补全验证
  - [x] 3.3 全量测试通过，0 回归（148 tests, 0 failures）

## Review Findings

- [x] [Review][Patch] 未使用的 `java.util.Objects` 导入 [StoryPersistenceService.java:14]
- [x] [Review][Defer] `MODEL_USED` 硬编码为 `dashscope/qwen-max` [StoryPersistenceService.java:39] — deferred, 后续配置化时统一处理

## Dev Notes

### 前置 Story 情报

**Story 2-1（LlmCallService）已完成：**
- `LlmCallService.java`（`com.rkos.common`）— LLM 调用封装
- `public String call(String prompt)` — 接收 String，返回 String
- `@Retryable` 重试：最多 3 次，指数退避 1s→2s→4s
- 异常分类：`LLM_QUOTA_EXCEEDED`（不重试）、`LLM_CALL_FAILED`（可重试）

**Story 2-2（PromptTemplateService）已完成：**
- `PromptTemplateService.java`（`com.rkos.common`）— Prompt 模板加载 + 变量替换
- `loadSystemPrompt(agentName)` / `loadUserTemplate(agentName)` / `render(template, variables)`

**Story 2-3（Genome 数据模型与 PostgreSQL 存储）已完成：**
- `RelationshipGenome.java` — 主实体，`@TableName("relationship_genomes")`，`autoResultMap = true`
  - 字段：`id`(BIGSERIAL), `storyId`(VARCHAR(36) UNIQUE), `agentVersion`, `genomeData`(JSONB), `overallConfidence`, `relationshipType`, `outcomeType`, `createdAt`, `updatedAt`
  - `genomeData` 使用 `JsonbTypeHandler` 序列化
- `GenomeData.java` — JSONB 内部结构 POJO，9 个维度
  - **已知问题**：`storyId` / `genomeId` 字段在 LLM 解析后为 null（Story 2-4 审查遗留）
- `GenomeMapper.java` — `extends BaseMapper<RelationshipGenome>`
  - 已有 `selectByStoryId(String)` default 方法
  - **无** `insertOrUpdate` / `updateByStoryId` 方法，本 Story 可能需要添加
- `V1__init_schema.sql` — `relationship_genomes` 表：
  - `story_id` VARCHAR(36) UNIQUE NOT NULL
  - `genome_data` JSONB NOT NULL
  - `created_at` / `updated_at` TIMESTAMP DEFAULT NOW()

**Story 2-4（StoryUnderstandingAgent）已完成：**
- `StoryUnderstandingAgent.java`（`com.rkos.modules.story.agent`）— 核心分析逻辑
- `public RelationshipGenome analyzeStory(String storyContent, String storyId)` — 返回 RelationshipGenome
- Agent **只负责分析和返回**，不做持久化
- 返回的 `RelationshipGenome` 中：
  - `storyId` 已设置（顶层字段）
  - `agentVersion` = "v1.0"
  - `genomeData.storyId` 和 `genomeData.genomeId` **为 null**（需本 Story 补全）
  - 扁平化列（`relationshipType`、`outcomeType`、`overallConfidence`）已同步

**现有 StoryService（Story 1-5）：**
- `StoryService.java`（`com.rkos.modules.story.service`）— 仅操作 MongoDB
- `submitStory()` 保存故事到 MongoDB，设置 `processingStatus = "PENDING"`（默认值）
- **不涉及** Genome 持久化逻辑

**现有 Story 模型（MongoDB）：**
- `Story.java`（`com.rkos.modules.story.model`）
- `processingStatus` — 默认 `"PENDING"`，有 `@Indexed`
- `ProcessingMetadata` 嵌套文档：`agentVersion`, `modelUsed`, `startedAt`, `completedAt`, `retryCount`, `errorMessage`
- `StoryMongoRepository.java`：`findByStoryId(String)` 返回 `Optional<Story>`

### GenomeData.storyId / genomeId 补全策略

Story 2-4 代码审查发现 `GenomeData.storyId` 和 `genomeId` 在 LLM 解析后为 null。
**本 Story 处理方式**：在 `persistGenome()` 方法中，写入 PostgreSQL 前检查并补全：

```java
// 补全 GenomeData 内部字段（Story 2-4 审查遗留修复）
if (genome.getGenomeData() != null) {
    GenomeData data = genome.getGenomeData();
    if (data.getStoryId() == null) {
        data.setStoryId(storyId);
    }
    // genomeId 可选，不强制填充（LLM 可能不输出）
}
```

> 这样保证 JSONB 内部 `story_id` 字段与顶层 `story_id` 一致，查询时不会产生歧义。

### 核心实现要点

**1. StoryPersistenceService 主方法签名：**

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class StoryPersistenceService {

    private final GenomeMapper genomeMapper;
    private final StoryMongoRepository storyMongoRepository;

    private static final String AGENT_VERSION = "v1.0";
    private static final String MODEL_USED = "dashscope/qwen-max";

    /**
     * 持久化 Genome 到 PostgreSQL，同时更新 MongoDB 处理状态。
     *
     * @param storyId 故事 ID（UUID）
     * @param genome  Agent 分析返回的 RelationshipGenome
     */
    public void persistGenome(String storyId, RelationshipGenome genome) {
        // 1. 输入校验
        // 2. 补全 GenomeData.storyId
        // 3. 更新 MongoDB → PROCESSING + startedAt
        // 4. 写入 PostgreSQL（GenomeMapper.insert）
        // 5. 成功 → 更新 MongoDB → COMPLETED + completedAt + metadata
        // 6. 失败 → 更新 MongoDB → FAILED + errorMessage
    }
}
```

**2. 处理状态状态机：**

```
PENDING (Story 提交时默认)
  → PROCESSING (开始持久化)
    → COMPLETED (PostgreSQL 写入 + MongoDB 更新均成功)
    → FAILED (任一环节失败)
```

**3. 双数据库协调策略（应用层）：**

```java
public void persistGenome(String storyId, RelationshipGenome genome) {
    Objects.requireNonNull(storyId, "storyId 不能为 null");
    Objects.requireNonNull(genome, "genome 不能为 null");

    LocalDateTime startedAt = LocalDateTime.now();

    // Step 1: 标记为 PROCESSING
    updateProcessingStatus(storyId, "PROCESSING", startedAt, null);

    try {
        // Step 2: 补全 GenomeData 内部字段
        supplementGenomeData(storyId, genome);

        // Step 3: 设置时间戳（PostgreSQL 层）
        LocalDateTime now = LocalDateTime.now();
        if (genome.getCreatedAt() == null) {
            genome.setCreatedAt(now);
        }
        genome.setUpdatedAt(now);

        // Step 4: 写入 PostgreSQL
        genomeMapper.insert(genome);

        // Step 5: 标记为 COMPLETED
        LocalDateTime completedAt = LocalDateTime.now();
        updateProcessingStatus(storyId, "COMPLETED", startedAt, completedAt);

        log.info("Genome 持久化成功: storyId={}", storyId);

    } catch (Exception e) {
        // Step 6: 标记为 FAILED + 记录 errorMessage
        log.error("Genome 持久化失败: storyId={}", storyId, e);
        updateProcessingStatusFailed(storyId, startedAt, e.getMessage());
        throw new RkosException("GENOME_PERSIST_FAILED",
                "Genome 持久化失败: " + storyId, e);
    }
}
```

**4. MongoDB 状态更新辅助方法：**

```java
private void updateProcessingStatus(String storyId, String status,
                                     LocalDateTime startedAt,
                                     LocalDateTime completedAt) {
    storyMongoRepository.findByStoryId(storyId).ifPresent(story -> {
        story.setProcessingStatus(status);
        story.setUpdatedAt(LocalDateTime.now());
        story.setProcessingMetadata(Story.ProcessingMetadata.builder()
                .agentVersion(AGENT_VERSION)
                .modelUsed(MODEL_USED)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .retryCount(0)
                .build());
        storyMongoRepository.save(story);
    });
}

private void updateProcessingStatusFailed(String storyId,
                                           LocalDateTime startedAt,
                                           String errorMessage) {
    storyMongoRepository.findByStoryId(storyId).ifPresent(story -> {
        story.setProcessingStatus("FAILED");
        story.setUpdatedAt(LocalDateTime.now());
        story.setProcessingMetadata(Story.ProcessingMetadata.builder()
                .agentVersion(AGENT_VERSION)
                .modelUsed(MODEL_USED)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .retryCount(0)
                .errorMessage(truncateError(errorMessage, 500))
                .build());
        storyMongoRepository.save(story);
    });
}
```

**5. 补偿机制说明：**

| 场景 | 行为 |
|------|------|
| PostgreSQL 写入成功 + MongoDB COMPLETED 更新成功 | 正常流程，无需补偿 |
| PostgreSQL 写入失败 | MongoDB 标记 FAILED + errorMessage，PostgreSQL 无残留 |
| PostgreSQL 写入成功 + MongoDB COMPLETED 更新失败 | PostgreSQL 数据保留，ERROR 日志记录，由后续定时任务或重新处理（Story 2-7）补偿 |
| MongoDB PROCESSING 更新失败 | 不写入 PostgreSQL，直接抛异常（MongoDB 不可用时不应继续） |

> **注意**：本 Story 不实现定时补偿扫描任务（属 Story 2-7 重新处理范围）。

**6. GenomeMapper 扩展（按需）：**

当前 `GenomeMapper` 只有 `selectByStoryId`，本 Story 主要使用 `BaseMapper.insert()`。
如果需要处理重复 storyId（重新处理场景），考虑添加：

```java
default int deleteByStoryId(String storyId) {
    return delete(new LambdaQueryWrapper<RelationshipGenome>()
            .eq(RelationshipGenome::getStoryId, storyId));
}
```

> **Story 2-5 范围内**只做 insert，重新处理的覆盖逻辑在 Story 2-7。

### 代码模式基线（必须遵循）

- **依赖注入**：构造函数注入，`private final` 字段 + `@RequiredArgsConstructor`
- **异常类**：`RkosException(String errorCode, String message)` 和 `(String errorCode, String message, Throwable cause)`
- **日志**：`@Slf4j`（Lombok）
- **Lombok 注解**：`@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- **空校验**：`Objects.requireNonNull(param, "描述")`

### 架构约束（必须遵循）

1. **文件位置**：Service 在 `com.rkos.modules.story.service` 包下
   - [Source: architecture.md #项目结构模式]
2. **双存储职责分离**：MongoDB 存原始数据，PostgreSQL 存结构化数据，应用层协调
   - [Source: architecture.md #决策 2：数据一致性控制]
3. **MongoDB 写入原始数据，PostgreSQL 写入结构化数据，职责不可混淆**
   - [Source: architecture.md #实现交接指南]
4. **所有异常使用 `RkosException`**，通过 `GlobalExceptionHandler` 统一处理
   - [Source: architecture.md #强制一致性规则 — 第4条]

### 本 Story 不涉及的范围（Defer 到后续 Story）

- **异步触发**（Story 2-6）— `@Async` 异步调用 Agent + 持久化
- **API 响应中的确认摘要**（Story 2-6）— 处理完成后 GET 返回摘要
- **重新处理覆盖**（Story 2-7）— `POST /api/v1/stories/{id}/reprocess`
- **定时补偿扫描**（Story 2-7 或后续）— 扫描 FAILED 状态自动重试
- **Genome 查询 API**（Epic 3）— REST 端点暴露
- **updated_at 自动触发器**（已知遗留，全表共性问题）

### Spring Boot 4.x 测试限制（已知问题）

- `@SpringBootTest` 全量上下文加载失败（flapdoodle embedded MongoDB 不兼容 Spring Boot 4.x）
- **解决方案**：使用纯 Mockito 单元测试，mock `GenomeMapper` 和 `StoryMongoRepository`
- 不需要启动 Spring 上下文，不需要真实数据库连接

### Project Structure Notes

新增文件：
```
src/main/java/com/rkos/modules/story/service/
└── StoryPersistenceService.java       # 本 Story 新增

src/test/java/com/rkos/modules/story/service/
└── StoryPersistenceServiceTest.java   # 本 Story 新增
```

已有文件（依赖，不修改）：
```
src/main/java/com/rkos/
├── common/
│   ├── LlmCallService.java            # Story 2-1（间接依赖）
│   ├── PromptTemplateService.java     # Story 2-2（间接依赖）
│   └── RkosException.java            # 异常类（依赖）
├── modules/story/
│   ├── agent/
│   │   └── StoryUnderstandingAgent.java # Story 2-4（间接依赖，本 Story 不调用）
│   ├── mapper/
│   │   └── GenomeMapper.java          # Story 2-3（依赖，本 Story 调用 insert）
│   ├── model/
│   │   ├── GenomeData.java            # Story 2-3（依赖，补全 storyId）
│   │   ├── RelationshipGenome.java    # Story 2-3（依赖）
│   │   └── Story.java                 # Story 1-4（依赖，更新 processingStatus）
│   ├── repository/
│   │   └── StoryMongoRepository.java  # Story 1-4（依赖）
│   └── service/
│       └── StoryService.java          # Story 1-5（不修改，独立服务）
```

### References

- [Source: epics.md #Story 2.5：Genome 持久化与处理状态管理]
- [Source: architecture.md #决策 2：数据一致性控制 — 应用层协调]
- [Source: architecture.md #项目结构模式 — modules/story/service/]
- [Source: architecture.md #实现交接指南 — MongoDB 写原始数据，PostgreSQL 写结构化数据]
- [Source: architecture.md #数据模型 — MongoDB stories 集合 + PostgreSQL relationship_genomes 表]
- [Source: modules/story/model/Story.java — 故事模型（processingStatus、ProcessingMetadata）]
- [Source: modules/story/model/RelationshipGenome.java — 基因组实体]
- [Source: modules/story/model/GenomeData.java — JSONB 内部结构]
- [Source: modules/story/mapper/GenomeMapper.java — MyBatis-Plus Mapper]
- [Source: modules/story/repository/StoryMongoRepository.java — MongoDB Repository]
- [Source: modules/story/agent/StoryUnderstandingAgent.java — Agent 返回 RelationshipGenome]
- [Source: db/migration/V1__init_schema.sql — relationship_genomes 表结构]
- [Source: deferred-work.md #story-2.4 — GenomeData.storyId/genomeId 未填充]
- [Source: 2-4-story-understanding-agent.md #Review Findings — Defer 项]

## Dev Agent Record

### Agent Model Used

Qoder AI Assistant

### Debug Log References

- MyBatis-Plus BaseMapper `insert(T)` 与 `insert(Collection<T>)` 重载歧义：mock 中 `any()` 需指定类型 `any(RelationshipGenome.class)`
- Mockito 共享可变对象陷阱：`findByStoryId` mock 返回同一 Story 引用时，ArgumentCaptor 捕获到最终状态而非中间状态，需返回不同副本

### Completion Notes List

- `StoryPersistenceService.java` 实现完整：双库协调（MongoDB PROCESSING → PostgreSQL insert → MongoDB COMPLETED/FAILED）
- 补偿机制：PostgreSQL 失败 → MongoDB FAILED；MongoDB PROCESSING 失败 → 不写 PostgreSQL；MongoDB COMPLETED 失败 → PostgreSQL 数据保留
- `GenomeData.storyId` 补全：persistGenome 写入前检查并填充 null 值
- 错误信息截断：500 字符上限 + "..."
- 14 个新增测试全部通过，全量 148 tests 0 回归

### File List

新增：
- `backend/src/main/java/com/rkos/modules/story/service/StoryPersistenceService.java`
- `backend/src/test/java/com/rkos/modules/story/service/StoryPersistenceServiceTest.java`

依赖（未修改）：
- `backend/src/main/java/com/rkos/common/RkosException.java`
- `backend/src/main/java/com/rkos/modules/story/mapper/GenomeMapper.java`
- `backend/src/main/java/com/rkos/modules/story/model/GenomeData.java`
- `backend/src/main/java/com/rkos/modules/story/model/RelationshipGenome.java`
- `backend/src/main/java/com/rkos/modules/story/model/Story.java`
- `backend/src/main/java/com/rkos/modules/story/repository/StoryMongoRepository.java`

### Change Log

- 2026-07-24: Story 2-5 实现完成 — StoryPersistenceService + 14 个单元测试
