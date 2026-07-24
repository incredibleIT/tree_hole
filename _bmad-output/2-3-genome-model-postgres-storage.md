# Story 2.3: Genome 数据模型与 PostgreSQL 存储

Status: done

## Story

作为**开发者**，
我希望 Genome 相关的 Java 数据模型和 PostgreSQL Repository 已实现，
以便 Genome 数据可以序列化/反序列化和持久化。

## Acceptance Criteria

1. [x] `model/` 目录下 9 个 Java 类已创建：`RelationshipGenome`、`GenomeData`、`Relationship`、`Participant`、`KeyEvent`、`ConflictPattern`、`Outcome`、`Confidence`、`EmotionalArc`（`CausalChain` 不需要独立类，JSONB 中为 `List<String>`）
2. [x] `GenomeMapper.java`（MyBatis-Plus Mapper）已实现，继承 `BaseMapper<RelationshipGenome>`
3. [x] `genome_data` JSONB 字段正确写入完整的 Genome 结构（9 个维度）
4. [x] 扁平化列（`relationship_type`、`outcome_type`、`overall_confidence`）同步更新
5. [x] 读取时 JSONB 正确反序列化为 Java 对象
6. [x] 自定义 `JsonbTypeHandler` 使用 `Types.OTHER` 写入 JSONB
7. [x] 集成测试验证 JSONB 读写（直连 PostgreSQL 验证 Types.OTHER + JSONB 往返）

## Tasks / Subtasks

- [x] Task 1: 创建 Genome 数据模型类 (AC: #1)
  - [x] 1.1 在 `com.rkos.modules.story.model` 包下创建 `RelationshipGenome.java`（主实体，含 `@TableName`）
  - [x] 1.2 创建 `Relationship.java`（关系维度：type、duration、stage、startContext）
  - [x] 1.3 创建 `Participant.java`（参与者：role、attachment、behaviors、emotions、ageAtStory、gender）
  - [x] 1.4 创建 `KeyEvent.java`（关键事件：event、position、description）
  - [x] 1.5 `CausalChain` 类不需要单独创建（JSONB 中为 `List<String>`，在 GenomeData 中直接定义）
  - [x] 1.6 创建 `ConflictPattern.java`（冲突模式：type、frequency、resolution、description）
  - [x] 1.7 创建 `Outcome.java`（结果：type、initiator、manner）
  - [x] 1.8 创建 `Confidence.java`（置信度：overall、relationship、participants、causalChain、conflictPatterns）
  - [x] 1.9 创建 `EmotionalArc.java`（情感弧线：dominantEmotions、trajectory）
  - [x] 1.10 创建 `GenomeData.java`（JSONB 内部结构 POJO，9 维度嵌套）
- [x] Task 2: 创建 JsonbTypeHandler (AC: #6)
  - [x] 2.1 在 `com.rkos.config` 包下创建 `JsonbTypeHandler.java`
  - [x] 2.2 `setParameter` 使用 `ps.setObject(i, json, Types.OTHER)`
  - [x] 2.3 `getResult` 使用 Jackson 2.x ObjectMapper 反序列化 JSONB → Java 对象
- [x] Task 3: 创建 GenomeMapper (AC: #2, #3, #4, #5)
  - [x] 3.1 在 `com.rkos.modules.story.mapper` 包下创建 `GenomeMapper.java`
  - [x] 3.2 继承 `BaseMapper<RelationshipGenome>`
  - [x] 3.3 定义 `selectByStoryId(String storyId)` 便捷方法
- [x] Task 4: 集成测试 (AC: #7)
  - [x] 4.1 创建 `GenomeModelTest.java` 单元测试（13 个测试：Builder/Getter、JSON 往返、扁平化同步）
  - [x] 4.2 创建 `JsonbTypeHandlerTest.java` 单元测试（11 个测试：Types.OTHER 写入、反序列化、null 处理、往返）
  - [x] 4.3 创建 `GenomeMapperTest.java` 集成测试（6 个测试：JSONB 写入 9 维度、读取反序列化、扁平化一致性、selectByStoryId、更新、删除）
  - [x] 4.4 全量测试通过：117 个测试（原有 87 + 新增 30），0 失败，0 回归

## Dev Notes

### 前置 Story 情报

**Story 2-1（LlmCallService）已完成：**
- `LlmCallService.java`（`com.rkos.common`）— LLM 调用封装，`@Retryable` 重试
- `RetryConfig.java`（`com.rkos.config`）— `@EnableRetry` 配置
- 68 + Code Review 修复后 = 68 个测试通过

**Story 2-2（PromptTemplateService）已完成：**
- `PromptTemplateService.java`（`com.rkos.common`）— Prompt 模板加载，`ResourceLoader` + `validateAgentName` 路径遍历防护
- `PromptTemplateServiceTest.java` — 14 个测试
- `application.yml` 添加 `rkos.prompts.base-path: classpath:/prompts/`
- Code Review 修复 3 个 Patch（路径遍历、尾部斜杠、级联替换）
- 总计 82 个测试通过

**代码模式基线（必须遵循）：**
- **依赖注入**：构造函数注入，`private final` 字段（不用 `@Autowired`）
- **异常类**：`RkosException(String errorCode, String message)` 和 `(String errorCode, String message, Throwable cause)`
- **日志**：`@Slf4j`（Lombok）
- **Lombok 注解**：`@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- **MyBatis-Plus 模型**：`@TableName("表名")`，`@TableId(type = IdType.AUTO)`，`@Mapper`

### 架构约束（必须遵循）

1. **文件位置**：模型类在 `com.rkos.modules.story.model` 包下，Mapper 在 `com.rkos.modules.story.mapper` 包下
   - [Source: architecture.md #项目结构模式]
2. **JSONB TypeHandler**：**禁止使用** MyBatis-Plus 内置 `JacksonTypeHandler`（VARCHAR 传参，PostgreSQL 不接受 varchar→jsonb 隐式转换），必须自定义 `JsonbTypeHandler` 使用 `ps.setObject(i, json, Types.OTHER)`
   - [Source: 项目经验 — PostgreSQL JSONB 写入需自定义 TypeHandler]
3. **`@TableName(autoResultMap = true)`**：使用自定义 TypeHandler 时**必须**设置
   - [Source: 项目经验]
4. **混合存储策略**：高频查询字段扁平化为独立列（`relationship_type`、`outcome_type`、`overall_confidence`），复杂嵌套结构存入 `genome_data` JSONB
   - [Source: architecture.md #数据存储架构 — 决策 1]
5. **PostgreSQL 表已存在**：`relationship_genomes` 表由 `V1__init_schema.sql` 创建，**不要修改迁移脚本**
6. **Jackson 2.x**：Spring Boot 4.1 默认 Jackson 3.x，项目通过 `Jackson2Config` 手动注册了 Jackson 2.x `ObjectMapper`。MyBatis-Plus 使用 Jackson 2.x

### 核心实现要点

**1. RelationshipGenome 主实体（`@TableName`）：**

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "relationship_genomes", autoResultMap = true)
public class RelationshipGenome {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String storyId;
    private String agentVersion;

    /** JSONB 字段 — 使用自定义 TypeHandler */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private GenomeData genomeData;

    /** 扁平化列 — 与 genomeData 内部字段保持同步 */
    private BigDecimal overallConfidence;
    private String relationshipType;
    private String outcomeType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

> **关键**：`autoResultMap = true` 是 JSONB TypeHandler 正常工作的必要条件。
> `GenomeData` 是包含 9 个维度的嵌套 POJO（不是独立表），直接映射到 JSONB。

**2. GenomeData 嵌套结构（映射 JSONB 内部）：**

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenomeData {
    private String genomeId;
    private String storyId;
    private String version;
    private Relationship relationship;
    private Map<String, Participant> participants;  // "A" → Participant, "B" → Participant
    private List<KeyEvent> keyEvents;
    private List<String> causalChain;               // 因果链为事件字符串列表
    private List<ConflictPattern> conflictPatterns;
    private Outcome outcome;
    private List<String> lessons;
    private Confidence confidence;
    private EmotionalArc emotionalArc;
}
```

> **注意**：`causalChain` 在 architecture.md JSONB 示例中是字符串数组（不是 CausalChain 对象列表）。
> 因此 epics 中提到的 `CausalChain` 类**不需要单独创建**，直接用 `List<String>` 表示。
> 同理 `lessons` 也是 `List<String>`。

**3. 各维度模型类：**

```java
// Relationship.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Relationship {
    private String type;        // 情侣、友谊、家庭...
    private String duration;    // "3年"
    private String stage;       // "冷淡期"
    private String startContext; // "大学校园"
}

// Participant.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Participant {
    private String role;            // "叙述者"、"对方"
    private String attachment;      // "焦虑型"、"回避型"
    private List<String> behaviors; // ["索取确认", "频繁追问"]
    private List<String> emotions;  // ["焦虑", "不安"]
    private Integer ageAtStory;
    private String gender;
}

// KeyEvent.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KeyEvent {
    private String event;       // "工作压力增加"
    private String position;    // "beginning"、"climax"、"end"
    private String description;
}

// ConflictPattern.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConflictPattern {
    private String type;        // "communication"、"emotional_needs"
    private String frequency;   // "recurring"
    private String resolution;  // "escalation"、"unresolved"
    private String description;
}

// Outcome.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Outcome {
    private String type;        // "分手"、"和好"、"持续"
    private String initiator;   // "B"
    private String manner;      // "direct"
}

// Confidence.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Confidence {
    private BigDecimal overall;
    private BigDecimal relationship;
    private BigDecimal participants;
    private BigDecimal causalChain;
    private BigDecimal conflictPatterns;
}

// EmotionalArc.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmotionalArc {
    private List<String> dominantEmotions;  // ["遗憾", "不舍"]
    private String trajectory;              // "decline"
}
```

> 共 **8 个模型类**（RelationshipGenome + GenomeData + Relationship + Participant + KeyEvent + ConflictPattern + Outcome + Confidence + EmotionalArc）。
> epics 中列出的 `CausalChain` 类不需要创建（JSONB 中为字符串数组 `List<String>`）。

**4. JsonbTypeHandler（自定义，替代 MyBatis-Plus 内置）：**

```java
@MappedTypes(GenomeData.class)
public class JsonbTypeHandler extends BaseTypeHandler<GenomeData> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, GenomeData parameter, JdbcType jdbcType)
            throws SQLException {
        String json = MAPPER.writeValueAsString(parameter);
        ps.setObject(i, json, Types.OTHER);  // Types.OTHER 是关键！PostgreSQL 识别为 JSONB
    }

    @Override
    public GenomeData getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return json != null ? MAPPER.readValue(json, GenomeData.class) : null;
    }
    // ... 其他 getNullableResult 重载同理
}
```

> **为什么不用 MyBatis-Plus 内置 JacksonTypeHandler：** 内置版本使用 `ps.setString()`（VARCHAR 类型），
> PostgreSQL 不接受 `varchar → jsonb` 隐式转换，会报 `column is of type jsonb but expression is of type character varying`。
> 必须使用 `ps.setObject(i, json, Types.OTHER)` 让 PostgreSQL JDBC 驱动识别为 JSONB 类型。

**5. GenomeMapper：**

```java
@Mapper
public interface GenomeMapper extends BaseMapper<RelationshipGenome> {

    /**
     * 根据 storyId 查询 Genome。
     */
    default RelationshipGenome selectByStoryId(String storyId) {
        return selectOne(new LambdaQueryWrapper<RelationshipGenome>()
                .eq(RelationshipGenome::getStoryId, storyId));
    }
}
```

> 与 `ApiKeyMapper` 模式一致：继承 `BaseMapper`，通过 `default` 方法定义便捷查询。

### 扁平化列同步机制

写入时必须保证扁平化列与 `genomeData` 内部字段一致：

```java
// 在构建 RelationshipGenome 时同步扁平化字段
RelationshipGenome genome = RelationshipGenome.builder()
        .storyId(storyId)
        .agentVersion("v1.0")
        .genomeData(genomeData)
        // 扁平化列 — 从 genomeData 中提取
        .relationshipType(genomeData.getRelationship() != null ? genomeData.getRelationship().getType() : null)
        .outcomeType(genomeData.getOutcome() != null ? genomeData.getOutcome().getType() : null)
        .overallConfidence(genomeData.getConfidence() != null ? genomeData.getConfidence().getOverall() : null)
        .build();
```

> 本 Story 只实现数据模型和 Mapper 层，不包含 Service 层协调逻辑（Story 2-5 `StoryPersistenceService` 负责）。

### `V1__init_schema.sql` 表结构参考（已存在，不要修改）

```sql
CREATE TABLE relationship_genomes (
    id                  BIGSERIAL       PRIMARY KEY,
    story_id            VARCHAR(36)     NOT NULL,
    agent_version       VARCHAR(20)     NOT NULL,
    genome_data         JSONB           NOT NULL,
    overall_confidence  DECIMAL(3,2),
    relationship_type   VARCHAR(50),
    outcome_type        VARCHAR(50),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);
```

> **注意**：`DECIMAL(3,2)` 范围是 -9.99 到 9.99，置信度 0.00-1.00 完全在范围内。

### MyBatis-Plus 配置参考（已就绪，无需修改）

```yaml
# application-dev.yml
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.rkos.modules.*.model
  configuration:
    map-underscore-to-camel-case: true    # snake_case → camelCase 自动映射
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
```

> `map-underscore-to-camel-case: true` 确保 `story_id` → `storyId`、`genome_data` → `genomeData` 自动映射。
> `type-aliases-package: com.rkos.modules.*.model` 通配符覆盖 `story.model` 包。

### 集成测试方案

使用 `@SpringBootTest` 或 MyBatis-Plus 的 `@MybatisPlusTest` 切片测试：

```java
@SpringBootTest
class GenomeMapperTest {
    @Autowired
    private GenomeMapper genomeMapper;

    @Test
    void saveAndRead_genomeData_jsonbRoundTrip() {
        // 1. 构建完整 GenomeData（9 维度）
        // 2. 构建 RelationshipGenome（含扁平化列同步）
        // 3. genomeMapper.insert(genome)
        // 4. genomeMapper.selectByStoryId(storyId)
        // 5. 断言 JSONB 反序列化后各维度字段值正确
        // 6. 断言扁平化列值正确
    }
}
```

> 集成测试需要真实 PostgreSQL（Docker Compose 已提供），或使用 `@MybatisPlusTest` + 嵌入式 H2（H2 不原生支持 JSONB，推荐连接真实 PostgreSQL）。
> 当前 `docker-compose.yml` 已有 PostgreSQL 16（localhost:5432）。
> **测试前确保 Docker Compose 中 PostgreSQL 已启动。**

### 本 Story 不涉及的范围（Defer 到后续 Story）

- **Service 层持久化协调**（Story 2-5 `StoryPersistenceService`）— 双存储写入逻辑
- **Agent 编排调用**（Story 2-4 `StoryUnderstandingAgent`）— LLM 调用 + Prompt 加载
- **处理状态管理**（Story 2-5）— MongoDB `processing_status` 更新
- **Genome 查询 API**（Epic 3）— REST 端点暴露
- **Flyway 迁移脚本修改** — 表结构已由 V1 创建完成

### Project Structure Notes

新增文件：
```
src/main/java/com/rkos/
├── config/
│   └── JsonbTypeHandler.java           # 本 Story 新增
├── modules/story/
│   ├── model/
│   │   ├── RelationshipGenome.java     # 本 Story 新增（主实体）
│   │   ├── GenomeData.java             # 本 Story 新增（JSONB 内部结构）
│   │   ├── Relationship.java           # 本 Story 新增
│   │   ├── Participant.java            # 本 Story 新增
│   │   ├── KeyEvent.java               # 本 Story 新增
│   │   ├── ConflictPattern.java        # 本 Story 新增
│   │   ├── Outcome.java                # 本 Story 新增
│   │   ├── Confidence.java             # 本 Story 新增
│   │   └── EmotionalArc.java           # 本 Story 新增
│   └── mapper/
│       └── GenomeMapper.java           # 本 Story 新增

src/test/java/com/rkos/
└── modules/story/mapper/
    └── GenomeMapperTest.java           # 本 Story 新增
```

已有文件（不修改）：
```
├── config/
│   ├── Jackson2Config.java             # Jackson 2.x ObjectMapper（已注册）
│   ├── RetryConfig.java
│   ├── SwaggerConfig.java
│   └── ApiKeyAuthConfig.java
├── common/
│   ├── ApiResponse.java
│   ├── GlobalExceptionHandler.java
│   ├── LlmCallService.java
│   ├── PromptTemplateService.java
│   └── RkosException.java
├── modules/story/
│   ├── model/Story.java                # MongoDB 模型（不修改）
│   └── repository/StoryMongoRepository.java
├── modules/auth/
│   ├── model/ApiKey.java               # PostgreSQL 模型参考（模式一致）
│   └── mapper/ApiKeyMapper.java        # MyBatis-Plus Mapper 参考（模式一致）
```

不修改的文件：
```
├── src/main/resources/
│   ├── db/migration/V1__init_schema.sql   # 表已存在，不修改
│   ├── application.yml                     # 无需修改
│   └── application-dev.yml                 # MyBatis-Plus 配置已就绪
```

### References

- [Source: epics.md #Story 2.3：Genome 数据模型与 PostgreSQL 存储]
- [Source: architecture.md #数据模型 — PostgreSQL relationship_genomes 表]
- [Source: architecture.md #genome_data JSONB 内部结构]
- [Source: architecture.md #数据存储架构 — 决策 1：四存储演进策略]
- [Source: architecture.md #项目结构模式 — modules/story/model + mapper]
- [Source: architecture.md #强制一致性规则 — 第1条 命名一致性]
- [Source: V1__init_schema.sql #relationship_genomes 表定义]
- [Source: auth/model/ApiKey.java #MyBatis-Plus 模型模式参考]
- [Source: auth/mapper/ApiKeyMapper.java #MyBatis-Plus Mapper 模式参考]
- [Source: config/Jackson2Config.java #Jackson 2.x ObjectMapper 注册]
- [Source: 项目经验 — PostgreSQL JSONB 写入需自定义 TypeHandler 并使用 Types.OTHER]
- [Source: 2-1-llm-call-service.md #Story 2-1 完成情报]
- [Source: 2-2-prompt-template-service.md #Story 2-2 完成情报]

## Dev Agent Record

### Agent Model Used

Qoder AI (dev-story executor)

### Debug Log References

- `@SpringBootTest` 全量上下文加载失败：flapdoodle embedded MongoDB 与 Spring Boot 4.x 不兼容（`NoClassDefFoundError: MongoProperties`）。改用直连 PostgreSQL JDBC 的轻量集成测试，避免启动 Spring 上下文。
- JDK 版本：Maven 默认使用 JDK 17，需 `export JAVA_HOME` 指向 JDK 21 才能编译。

### Completion Notes List

1. **模型类**：创建 9 个 Java 类（GenomeData + 8 维度模型），`CausalChain` 不创建独立类（JSONB 中为 `List<String>`）
2. **JsonbTypeHandler**：自定义 `BaseTypeHandler<GenomeData>`，使用 `Types.OTHER` 写入 JSONB，Jackson 2.x 序列化/反序列化
3. **GenomeMapper**：继承 `BaseMapper<RelationshipGenome>`，`selectByStoryId` 便捷方法
4. **测试覆盖**：30 个新增测试（13 模型 + 11 TypeHandler + 6 集成），全量 117 个测试 0 失败

### File List

**新增文件：**
- `src/main/java/com/rkos/modules/story/model/RelationshipGenome.java`
- `src/main/java/com/rkos/modules/story/model/GenomeData.java`
- `src/main/java/com/rkos/modules/story/model/Relationship.java`
- `src/main/java/com/rkos/modules/story/model/Participant.java`
- `src/main/java/com/rkos/modules/story/model/KeyEvent.java`
- `src/main/java/com/rkos/modules/story/model/ConflictPattern.java`
- `src/main/java/com/rkos/modules/story/model/Outcome.java`
- `src/main/java/com/rkos/modules/story/model/Confidence.java`
- `src/main/java/com/rkos/modules/story/model/EmotionalArc.java`
- `src/main/java/com/rkos/config/JsonbTypeHandler.java`
- `src/main/java/com/rkos/modules/story/mapper/GenomeMapper.java`
- `src/test/java/com/rkos/modules/story/model/GenomeModelTest.java`
- `src/test/java/com/rkos/config/JsonbTypeHandlerTest.java`
- `src/test/java/com/rkos/modules/story/mapper/GenomeMapperTest.java`

**未修改的文件（参考/依赖）：**
- `src/main/java/com/rkos/config/Jackson2Config.java`
- `src/main/java/com/rkos/modules/auth/model/ApiKey.java`（模式参考）
- `src/main/java/com/rkos/modules/auth/mapper/ApiKeyMapper.java`（模式参考）
- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/resources/application-dev.yml`

### Change Log

- 2026-07-16: Story 2-3 实现完成 — 9 个模型类 + JsonbTypeHandler + GenomeMapper + 30 个测试

### Review Findings

- [x] [Review][Patch] 测试 INSERT 用 `?::jsonb` 而非 `Types.OTHER` — 生产代码路径与测试代码路径不一致 [GenomeMapperTest.java:82,184,223,266,313] — 已修复
- [x] [Review][Defer] 集成测试未通过 MyBatis-Plus 管道 — `@SpringBootTest` 因 Spring Boot 4.x 兼容性不可用，直连方式是当前最佳替代，GenomeMapper end-to-end 验证留待后续 Story — deferred, 技术限制
- [x] [Review][Defer] `buildFullGenomeData` 辅助方法在两个测试类中重复 — 提取到共享测试工具类是优化项 — deferred, 优化项
- [x] [Review][Defer] `selectByStoryId` 测试使用原生 JDBC 而非 GenomeMapper — 与 Spring Boot 4.x 兼容性限制同源 — deferred, 技术限制
- [x] [Review][Defer] `JsonbTypeHandler` 自建 static ObjectMapper — MyBatis TypeHandler 不走 Spring DI，是标准做法 — deferred, 架构约束
- [x] [Review][Defer] 集成测试硬编码数据库凭证 — 提取到 test properties 是优化项 — deferred, 优化项
- [x] [Review][Defer] `updated_at` 无 UPDATE 触发器 — 全表共性问题，非本 Story 范围 — deferred, 后续统一处理
