# Story 2.8: 测试数据种子

Status: done

## Story

作为**开发者/测试者**，
我希望有预设的测试数据种子，
以便在不依赖 LLM 的情况下测试 Genome 查询和持久化流程。

## Acceptance Criteria

1. [x] `src/test/resources/seed/` 下存在种子数据 JSON 文件（`seed-genomes.json` + `seed-stories.json`）
2. [x] `SeedDataLoader` 工具类提供 `loadGenomes()` 和 `loadStories()` 方法，解析 JSON 返回 Java 对象列表
3. [x] 种子数据包含 5 条预设 Genome，覆盖不同 `relationship_type`（情侣、友谊、家庭、同事、师生）
4. [x] 种子数据覆盖不同 `outcome_type`（分手、和好、持续、疏远、感恩）
5. [x] 种子数据的 MongoDB Story 与 PostgreSQL Genome 通过 `storyId` 一一对应
6. [x] 种子数据仅用于测试环境（`src/test/`），不影响生产
7. [x] 单元测试验证 `SeedDataLoader` 正确解析所有种子数据，`GenomeMapperTest` 集成测试使用种子数据验证写入和读取

## Tasks / Subtasks

- [x] Task 1: 创建种子数据 JSON 文件 (AC: #1, #3, #4, #5)
  - [x] 1.1 创建 `src/test/resources/seed/seed-genomes.json`：5 条 Genome 数据（含完整 9 维度 `genomeData`）
  - [x] 1.2 创建 `src/test/resources/seed/seed-stories.json`：5 条对应的 MongoDB Story 数据
  - [x] 1.3 保证每条 Genome 的 `storyId` 与对应 Story 的 `storyId` 一致
  - [x] 1.4 扁平化列（`relationshipType`、`outcomeType`、`overallConfidence`）与 `genomeData` 内部字段一致
- [x] Task 2: 创建 SeedDataLoader 工具类 (AC: #2, #6)
  - [x] 2.1 在 `src/test/java/com/rkos/modules/story/seed/` 下创建 `SeedDataLoader.java`
  - [x] 2.2 `loadGenomes()` — 从 classpath `seed/seed-genomes.json` 读取并反序列化为 `List<RelationshipGenome>`
  - [x] 2.3 `loadStories()` — 从 classpath `seed/seed-stories.json` 读取并反序列化为 `List<Story>`
  - [x] 2.4 使用 Jackson 2.x `ObjectMapper`（与项目一致：`com.fasterxml.jackson`），注册 `JavaTimeModule`
- [x] Task 3: 单元测试 (AC: #7)
  - [x] 3.1 创建 `SeedDataLoaderTest.java`：验证 5 条 Genome 解析正确（9 维度完整、扁平化列一致）
  - [x] 3.2 验证 5 条 Story 解析正确（storyId 与 Genome 一一对应、processingStatus=COMPLETED）
  - [x] 3.3 在 `GenomeMapperTest.java` 中新增种子数据写入+读回测试（直连 PostgreSQL 集成验证）
  - [x] 3.4 全量测试通过，0 回归
- [x] Task 4: 提取共享测试工具方法 (AC: #7)
  - [x] 4.1 提取 `GenomeMapperTest.buildFullGenomeData()` 到 `src/test/java/com/rkos/modules/story/TestGenomeFactory.java`（消除重复，`SeedDataLoaderTest` 和 `GenomeMapperTest` 共享）
  - [x] 4.2 `GenomeMapperTest` 改为引用 `TestGenomeFactory`

### Review Findings

- [x] [Review][Patch] 异常路径测试未实际测试 SeedDataLoader 的逻辑 [SeedDataLoaderTest.java:257-268] — 已修复，使用反射调用 private loadList
- [x] [Review][Patch] contentLength 与 content 实际长度未验证 [seed-stories.json] — 已修复，修正 5 条数据 + 新增一致性测试
- [x] [Review][Defer] ObjectMapper 配置重复（SeedDataLoader vs GenomeMapperTest）— deferred, pre-existing
- [x] [Review][Defer] seed storyId 非 UUID 格式 — deferred, 当前无 UUID 校验， Epic 3 注意

## Dev Notes

### 前置 Story 情报

**Story 2-7（故事重新处理）已完成 — 165 tests, 0 failures：**
- `GenomeMapper.upsertByStoryId()` — delete + insert 覆盖写入
- `StoryPersistenceService.repersistGenome()` — REPROCESSING → COMPLETED/FAILED
- `StoryProcessingService.reprocessStoryAsync()` — @Async 异步编排
- `StoryService.reprocessStory()` — 存在校验 + 409 冲突检查
- `StoryController` — `POST /{storyId}/reprocess` (202 Accepted)

**Story 2-3（Genome 数据模型）已完成：**
- `GenomeMapperTest.java` 包含 `buildFullGenomeData(String storyId)` 辅助方法（~70 行），构建完整 9 维度 GenomeData
- 该方法在 `GenomeMapperTest` 中重复使用，是本 Story 提取到共享工具类的候选
- [Source: code review of 2-3 — deferred: "`buildFullGenomeData` 辅助方法重复，提取到共享测试工具类是优化项"]

**现有测试约束（Spring Boot 4.x）：**
- `@SpringBootTest` 全量上下文不可用（flapdoodle embedded MongoDB 不兼容 Spring Boot 4.x）
- 所有单元测试使用纯 Mockito（`@ExtendWith(MockitoExtension.class)`）
- `GenomeMapperTest` 使用直连 PostgreSQL（`localhost:5432`，需 Docker Compose 运行）
- 种子数据加载器是纯 Java 工具类，不依赖 Spring 上下文

**现有测试资源：**
- `src/test/resources/application.properties` — flapdoodle 配置
- `src/test/resources/prompts/test-agent/` — 测试用 Prompt 模板

### 核心实现要点

**1. 种子数据 JSON 格式（seed-genomes.json）：**

```json
[
  {
    "storyId": "seed-story-001",
    "agentVersion": "v1.0",
    "genomeData": {
      "genomeId": "seed-genome-001",
      "storyId": "seed-story-001",
      "version": "v1.0",
      "relationship": { "type": "情侣", "duration": "3年", "stage": "冷淡期", "startContext": "大学校园" },
      "participants": { "A": { ... }, "B": { ... } },
      "keyEvents": [ ... ],
      "causalChain": [ ... ],
      "conflictPatterns": [ ... ],
      "outcome": { "type": "分手", "initiator": "B", "manner": "direct" },
      "lessons": [ ... ],
      "confidence": { "overall": 0.85, ... },
      "emotionalArc": { "dominantEmotions": [...], "trajectory": "decline" }
    },
    "overallConfidence": 0.85,
    "relationshipType": "情侣",
    "outcomeType": "分手",
    "createdAt": "2026-07-15T10:00:00",
    "updatedAt": "2026-07-15T10:00:00"
  },
  ...
]
```

**2. 种子数据覆盖矩阵：**

| # | storyId | relationshipType | outcomeType | overallConfidence | 故事主题 |
|---|---------|-----------------|-------------|-------------------|---------|
| 1 | seed-story-001 | 情侣 | 分手 | 0.85 | 校园恋爱→冷淡→分手 |
| 2 | seed-story-002 | 友谊 | 和好 | 0.78 | 发小因误会冷战→解释和好 |
| 3 | seed-story-003 | 家庭 | 持续 | 0.92 | 父子关系从紧张到理解 |
| 4 | seed-story-004 | 同事 | 疏远 | 0.70 | 合作伙伴因利益分歧疏远 |
| 5 | seed-story-005 | 师生 | 感恩 | 0.88 | 导师与学生深厚师生情 |

**3. SeedDataLoader 工具类：**

```java
/**
 * 种子数据加载器（测试工具类）。
 * <p>
 * 从 classpath 读取 JSON 种子数据文件，反序列化为 Java 对象列表。
 * 仅用于测试环境，不依赖 Spring 上下文。
 */
public final class SeedDataLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private SeedDataLoader() {} // 工具类禁止实例化

    public static List<RelationshipGenome> loadGenomes() {
        return loadList("seed/seed-genomes.json", RelationshipGenome.class);
    }

    public static List<Story> loadStories() {
        return loadList("seed/seed-stories.json", Story.class);
    }

    private static <T> List<T> loadList(String resourcePath, Class<T> type) {
        try (InputStream is = SeedDataLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) throw new IllegalStateException("种子数据文件不存在: " + resourcePath);
            return MAPPER.readValue(is,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new UncheckedIOException("种子数据加载失败: " + resourcePath, e);
        }
    }
}
```

**4. TestGenomeFactory 共享工厂（从 GenomeMapperTest 提取）：**

```java
/**
 * 测试用 Genome 数据工厂（共享工具类）。
 * <p>
 * 提供构建完整 GenomeData 的辅助方法，消除 GenomeMapperTest / SeedDataLoaderTest 中的重复代码。
 */
public final class TestGenomeFactory {
    private TestGenomeFactory() {}

    /** 构建完整 9 维度 GenomeData（情侣/分手场景） */
    public static GenomeData buildFullGenomeData(String storyId) { ... }

    /** 构建简化版 GenomeData（仅含必填字段） */
    public static GenomeData buildMinimalGenomeData(String storyId) { ... }
}
```

**5. GenomeMapperTest 集成测试（种子数据写入+读回）：**

```java
@Test
@Order(7)
void seedData_writeAndReadBack_allFiveGenomes() throws Exception {
    List<RelationshipGenome> genomes = SeedDataLoader.loadGenomes();
    assertThat(genomes).hasSize(5);

    for (RelationshipGenome genome : genomes) {
        String json = MAPPER.writeValueAsString(genome.getGenomeData());
        // 清理 + 写入 + 读回验证（直连 PostgreSQL）
        ...
    }
}
```

### 代码模式基线（必须遵循）

- **测试文件位置**：`src/test/java/com/rkos/modules/story/seed/` 和 `src/test/java/com/rkos/modules/story/`
- **测试资源位置**：`src/test/resources/seed/`
- **Jackson 2.x**：`com.fasterxml.jackson.databind.ObjectMapper`（非 Jackson 3.x `tools.jackson`）
- **JavaTimeModule**：`com.fasterxml.jackson.datatype.jsr310.JavaTimeModule`（`LocalDateTime` 序列化/反序列化）
- **工具类**：`private` 构造函数 + `static` 方法，禁止实例化
- **Lombok 注解**：模型类 `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- **不使用 Spring 上下文**：所有测试为纯 JUnit 5 + Mockito 或直连 JDBC

### 架构约束（必须遵循）

1. **种子数据仅在 `src/test/`**：不放入 `src/main/resources/`，保证不影响生产
   - [Source: epics.md #Story 2.8 验收标准 — "种子数据仅用于测试环境，不影响生产"]
2. **JSONB 序列化使用 `Types.OTHER`**：PostgreSQL 写入 JSONB 必须通过 `ps.setObject(i, json, Types.OTHER)`
   - [Source: architecture.md #数据模型 — PostgreSQL JSONB]
3. **扁平化列与 JSONB 一致性**：`relationshipType`、`outcomeType`、`overallConfidence` 必须与 `genomeData` 内部对应字段一致
   - [Source: architecture.md #数据模型 — 混合存储策略]
4. **storyId 关联**：MongoDB `stories.story_id` 与 PostgreSQL `relationship_genomes.story_id` 通过 UUID 字符串桥接
   - [Source: architecture.md #跨数据库关联策略]

### 本 Story 不涉及的范围

- **Genome 查询 API**（Epic 3）— 种子数据为 Epic 3 查询测试准备数据，但查询端点本身不在本 Story
- **@SpringBootTest 集成测试**（Spring Boot 4.x 兼容后）— 种子数据加载器不依赖 Spring 上下文
- **数据初始化脚本**（Flyway 迁移）— 种子数据不通过 Flyway 加载，仅在测试中按需加载

### Spring Boot 4.x 测试限制（已知问题）

- `@SpringBootTest` 全量上下文加载失败（flapdoodle embedded MongoDB 不兼容 Spring Boot 4.x）
- **解决方案**：`SeedDataLoader` 为纯 Java 工具类，不依赖 Spring 上下文
- `GenomeMapperTest` 集成测试需真实 PostgreSQL（Docker Compose `localhost:5432`）

### Project Structure Notes

新增文件：
```
src/test/resources/seed/
├── seed-genomes.json                          # 5 条 Genome 种子数据
└── seed-stories.json                          # 5 条 Story 种子数据

src/test/java/com/rkos/modules/story/
├── seed/
│   └── SeedDataLoader.java                    # 种子数据加载工具类
├── TestGenomeFactory.java                     # 共享测试数据工厂（从 GenomeMapperTest 提取）
└── seed/
    └── SeedDataLoaderTest.java                # 种子数据加载单元测试
```

修改文件：
```
src/test/java/com/rkos/modules/story/mapper/
└── GenomeMapperTest.java                      # 引用 TestGenomeFactory + 新增种子数据集成测试
```

已有文件（依赖，不修改）：
```
src/main/java/com/rkos/modules/story/model/
├── RelationshipGenome.java                    # Genome 实体
├── GenomeData.java                            # JSONB 内部结构
├── Story.java                                 # 故事领域模型
├── Relationship.java / Participant.java / KeyEvent.java / ...  # 9 维度模型
src/main/java/com/rkos/config/
└── JsonbTypeHandler.java                      # JSONB TypeHandler（参考序列化逻辑）
```

### References

- [Source: epics.md #Story 2.8：测试数据种子]
- [Source: architecture.md #数据模型 — MongoDB stories 集合 + PostgreSQL relationship_genomes 表]
- [Source: architecture.md #跨数据库关联策略 — UUID 字符串桥接]
- [Source: code review of 2-3 — deferred: buildFullGenomeData 重复代码提取到共享工具类]
- [Source: modules/story/mapper/GenomeMapperTest.java — buildFullGenomeData 辅助方法（提取源）]
- [Source: config/JsonbTypeHandler.java — JSONB 序列化参考]

## Dev Agent Record

### Agent Model Used

Qoder (AI Dev Agent)

### Debug Log References

无调试问题，所有任务一次通过。

### Completion Notes List

- **187 tests, 0 failures, 0 errors**（基线 165 + 新增 22 SeedDataLoaderTest 单元测试 + GenomeMapperTest 种子数据集成测试）
- Task 4（提取 TestGenomeFactory）作为依赖项优先完成，消除 GenomeMapperTest 中 ~70 行重复代码（解决 Story 2-3 代码审查延迟项）
- Task 1 创建 5 条完整 9 维度种子数据（情侣/分手、友谊/和好、家庭/持续、同事/疏远、师生/感恩）
- Task 2 实现纯 Java SeedDataLoader（不依赖 Spring 上下文），使用 Jackson 2.x + JavaTimeModule
- Task 3.1-3.2 SeedDataLoaderTest 22 个测试验证解析正确性、9 维度完整性、扁平化列一致性、Story-Genome 一一对应
- Task 3.3 GenomeMapperTest 新增 @Order(7) seedData_writeAndReadBack_allFiveGenomes 集成测试，直连 PostgreSQL 验证种子数据写入+读回

### File List

新增文件：
- `backend/src/test/resources/seed/seed-genomes.json` — 5 条 Genome 种子数据（完整 9 维度）
- `backend/src/test/resources/seed/seed-stories.json` — 5 条 Story 种子数据
- `backend/src/test/java/com/rkos/modules/story/seed/SeedDataLoader.java` — 种子数据加载工具类
- `backend/src/test/java/com/rkos/modules/story/seed/SeedDataLoaderTest.java` — 种子数据加载单元测试（22 tests）
- `backend/src/test/java/com/rkos/modules/story/TestGenomeFactory.java` — 共享测试数据工厂

修改文件：
- `backend/src/test/java/com/rkos/modules/story/mapper/GenomeMapperTest.java` — 引用 TestGenomeFactory + 新增种子数据集成测试

### Change Log

- 2026-07-20: Story 2-8 开发完成 — 种子数据 JSON + SeedDataLoader + TestGenomeFactory + 22 单元测试 + 集成测试

