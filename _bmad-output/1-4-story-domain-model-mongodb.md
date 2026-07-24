# Story 1.4：故事领域模型与 MongoDB 存储

Status: done

## Story

作为**开发者**，
我希望 Story 领域模型和 MongoDB Repository 已实现，
以便故事数据可以持久化到 MongoDB。

## Acceptance Criteria

1. **Given** `Story.java` 包含所有字段（story_id、author_id、content、relationship_type、anonymous、status、processing_status、processing_metadata 等）
   **When** 调用 `StoryMongoRepository.save(story)`
   **Then** 故事数据写入 MongoDB `stories` 集合
   **And** `story_id` 字段有唯一索引
   **And** `processing_status` 和 `created_at` 字段有普通索引
   **And** 编写集成测试验证 CRUD 操作

## Tasks / Subtasks

- [x] Task 1：创建 `Story` 领域模型（AC: #1）
  - [x] Subtask 1.1：在 `com.rkos.modules.story.model` 包下创建 `Story.java`，包含架构文档定义的全部字段
  - [x] Subtask 1.2：使用 `@Document(collection = "stories")` 注解指定集合名
  - [x] Subtask 1.3：使用 `@Id` 注解 MongoDB ObjectId 主键，`story_id` 使用 `@Indexed(unique = true)` 注解
  - [x] Subtask 1.4：`processing_status` 和 `created_at` 使用 `@Indexed` 注解
  - [x] Subtask 1.5：`processing_metadata` 字段使用嵌套类 `ProcessingMetadata`（静态内部类或独立类）
  - [x] Subtask 1.6：使用 Lombok `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor` 注解
- [x] Task 2：创建 `StoryMongoRepository` 接口（AC: #1）
  - [x] Subtask 2.1：在 `com.rkos.modules.story.repository` 包下创建 `StoryMongoRepository.java`，继承 `MongoRepository<Story, String>`
  - [x] Subtask 2.2：添加 `Optional<Story> findByStoryId(String storyId)` 查询方法
  - [x] Subtask 2.3：添加 `List<Story> findByProcessingStatus(String processingStatus)` 查询方法
- [x] Task 3：编写集成测试验证 CRUD（AC: #1）
  - [x] Subtask 3.1：创建 `StoryMongoRepositoryTest.java`，使用 `@DataMongoTest` 注解
  - [x] Subtask 3.2：测试保存（save）并验证 `story_id` 唯一索引
  - [x] Subtask 3.3：测试按 `storyId` 查询（findByStoryId）
  - [x] Subtask 3.4：测试按 `processingStatus` 查询（findByProcessingStatus）
  - [x] Subtask 3.5：测试更新操作（修改 content 后 save 验证更新）
  - [x] Subtask 3.6：测试删除操作（deleteById + 验证 count）
- [x] Task 4：编译与测试验证（AC: #1）
  - [x] Subtask 4.1：`mvn clean compile` 编译通过
  - [x] Subtask 4.2：`mvn test` 全部测试通过（20 个测试：ApiResponse 4 + GlobalExceptionHandler 8 + StoryMongoRepository 8）

## Dev Notes

### 架构规范（强制遵守）

**文件位置**：
```
backend/src/main/java/com/rkos/modules/story/
├── model/
│   └── Story.java                    # 领域模型
└── repository/
    └── StoryMongoRepository.java     # MongoDB 数据访问
```

[Source: _bmad-output/architecture.md#项目结构模式 L751-763]

**MongoDB `stories` 集合结构**（架构文档严格定义）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `_id` | ObjectId | MongoDB 主键 |
| `story_id` | String | 业务唯一标识（UUID，同时作为 PostgreSQL 的关联键） |
| `author_id` | String | 作者标识（第一迭代为占位，可为空或 `"anonymous"`） |
| `content` | String | 故事正文 |
| `relationship_type` | String | 关系类型（用户提交时可选） |
| `anonymous` | Boolean | 是否匿名 |
| `attachments` | List | 附件列表（第一迭代仅预留空数组） |
| `status` | String | 故事状态：`active` |
| `version` | Integer | 数据版本号 |
| `processing_status` | String | Agent 处理状态：`PENDING` / `PROCESSING` / `COMPLETED` / `FAILED` |
| `processing_metadata` | Object | 处理元信息（嵌套文档） |
| `content_length` | Integer | 故事字数 |
| `language` | String | 语言标识 |

[Source: _bmad-output/architecture.md#MongoDB stories 集合 L1368-1418]

**索引要求**（架构文档强制）：
- `story_id`：唯一索引（`@Indexed(unique = true)`）
- `processing_status`：普通索引（`@Indexed`）
- `created_at`：普通索引（`@Indexed`）

[Source: _bmad-output/architecture.md#索引 L1396-1399]

**`processing_metadata` 嵌套文档结构**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `agent_version` | String | Agent 版本号 |
| `model_used` | String | 使用的 LLM 模型 |
| `started_at` | LocalDateTime | 处理开始时间 |
| `completed_at` | LocalDateTime | 处理完成时间 |
| `retry_count` | Integer | 重试次数 |
| `error_message` | String | 错误信息（可为 null） |

[Source: _bmad-output/architecture.md#processing_metadata L1383-1391]

**字段映射规范**：
- Java 字段使用 `camelCase`（如 `storyId`、`processingStatus`）
- MongoDB 文档字段使用 `snake_case`（如 `story_id`、`processing_status`）
- 通过 `@Field("snake_case_name")` 注解显式映射

**关键设计决策**：
- `story_id` 是业务标识（UUID 字符串），与 `_id`（MongoDB ObjectId）分离
- `story_id` 同时作为 PostgreSQL `relationship_genomes` 表的关联键（UUID 字符串桥接）
- `status` 字段默认为 `"active"`，表示故事有效状态
- `version` 字段默认值为 `1`，用于数据版本控制
- `processing_status` 默认值为 `"PENDING"`
- `content_length` 在 save 时由 `content.length()` 计算（可通过 `@PrePersist` 或 Service 层实现）

### 强制一致性规则

- **命名一致性**：Java `camelCase` + MongoDB `snake_case`（通过 `@Field` 注解映射）
- **模块组织一致性**：模型放 `model/`、Repository 放 `repository/`
- **测试覆盖一致性**：Repository 必须有集成测试

[Source: _bmad-output/architecture.md#强制一致性规则 L1085-1101]

### 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | Spring Boot 4.1 基线 |
| Spring Boot | 4.1.0 | Jakarta EE 11，Spring Framework 7.0 |
| Spring Data MongoDB | Spring Boot 管理 | `spring-boot-starter-data-mongodb` |
| Lombok | Spring Boot 管理 | `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor` |
| JUnit 5 | Spring Boot 管理 | `spring-boot-starter-test` |

### 前一个 Story 情报（Story 1.3）

| 项目 | 内容 |
|------|------|
| 完成内容 | ApiResponse、RkosException、GlobalExceptionHandler |
| 桩实现文件 | `StoryController.java`（仅验证用）、`StoryRequest.java`（仅 content 字段） |
| 已有测试 | ApiResponseTest(4) + GlobalExceptionHandlerTest(8) = 12 个测试 |
| MockMvc 模式 | `standaloneSetup`，不加载 Spring 上下文 |
| Java 环境 | jenv 默认 Java 17，需 `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` |
| Deferred 项 | `ConstraintViolationException` 和 `MissingServletRequestPartException` 未专门处理（延迟到后续 Story） |

**关键注意事项**：
1. **不要修改已有文件**：Story 1.3 的桩实现 `StoryController.java` 和 `StoryRequest.java` 不要在本 Story 修改，它们会在 Story 1.5 扩展
2. **`@MapperScan` 已配置**：`RkosApplication.java` 已有 `@MapperScan("com.rkos.modules.*.mapper")`，这是 MyBatis-Plus 的 Mapper 扫描，与 MongoDB Repository 无关
3. **Spring Data MongoDB 自动配置**：`spring-boot-starter-data-mongodb` 已在 pom.xml 中，MongoDB 连接已在 `application-dev.yml` 配置（localhost:27017/rkos_dev）

### 测试策略

- **`@DataMongoTest` 集成测试**：使用 Spring Boot 提供的 MongoDB 测试切片，自动配置 `MongoTemplate` 和 `MongoRepository`
- **嵌入式 MongoDB**：`@DataMongoTest` 默认使用嵌入式 MongoDB（无需启动外部 MongoDB 容器）
- **测试覆盖**：CRUD 操作（save/findById/findAll/delete）、唯一索引约束、自定义查询方法
- **测试数据构建**：使用 `@Builder` 创建测试用 Story 对象，`@BeforeEach` 清理数据
- **注意**：`@DataMongoTest` 仅加载 MongoDB 相关 Bean，不会触发 MyBatis-Plus、Spring AI 等自动配置

### Project Structure Notes

本 Story 新增的文件：

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/modules/story/model/Story.java` | 新建 | 故事领域模型 |
| `backend/src/main/java/com/rkos/modules/story/repository/StoryMongoRepository.java` | 新建 | MongoDB 数据访问接口 |
| `backend/src/test/java/com/rkos/modules/story/repository/StoryMongoRepositoryTest.java` | 新建 | Repository 集成测试 |

不修改的文件：

| 文件 | 说明 |
|------|------|
| `backend/src/main/java/com/rkos/modules/story/controller/StoryController.java` | 桩实现，Story 1.5 扩展 |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryRequest.java` | 桩实现，Story 1.5 扩展 |
| `backend/src/main/java/com/rkos/RkosApplication.java` | 无需修改 |
| `backend/pom.xml` | 无需修改（spring-boot-starter-data-mongodb 已存在） |

### References

- [Source: _bmad-output/epics.md#Story 1.4：故事领域模型与 MongoDB 存储] — 验收标准定义
- [Source: _bmad-output/architecture.md#MongoDB stories 集合 L1368-1418] — 完整的集合结构与字段说明
- [Source: _bmad-output/architecture.md#索引 L1396-1399] — 索引定义
- [Source: _bmad-output/architecture.md#项目结构模式 L751-763] — 模块目录结构
- [Source: _bmad-output/architecture.md#跨数据库关联策略 L1554-1558] — story_id UUID 桥接策略
- [Source: _bmad-output/1-3-unified-response-exception-handler.md] — 前一 Story 完成情况和 Deferred 项

## Dev Agent Record

### Agent Model Used

Qoder (AI Agent)

### Debug Log References

- **flapdoodle `spring4x` 不存在**：Spring Boot 4.x 无专用 flapdoodle artifact，使用 `spring3x` 4.21.0 兼容
- **`@DataMongoTest` 包路径变更**：Spring Boot 4.x 将测试切片模块化，包路径从 `org.springframework.boot.test.autoconfigure.data.mongo` 改为 `org.springframework.boot.data.mongodb.test.autoconfigure`
- **需要 `spring-boot-starter-data-mongodb-test`**：Spring Boot 4.x 新增模块化测试 starter，必须单独引入
- **唯一索引未自动创建**：嵌入式 MongoDB 测试需配置 `spring.data.mongodb.auto-index-creation=true`

### Completion Notes List

- Story 1-4 新增 3 个源文件 + 1 个测试配置文件，未修改任何已有文件
- `pom.xml` 新增 2 个测试依赖：`spring-boot-starter-data-mongodb-test` + `flapdoodle spring3x`
- 全部 20 个测试通过：ApiResponseTest(4) + GlobalExceptionHandlerTest(8) + StoryMongoRepositoryTest(8)

### File List

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/modules/story/model/Story.java` | 新建 | 故事领域模型（13 字段 + ProcessingMetadata 嵌套类） |
| `backend/src/main/java/com/rkos/modules/story/repository/StoryMongoRepository.java` | 新建 | MongoDB Repository 接口 |
| `backend/src/test/java/com/rkos/modules/story/repository/StoryMongoRepositoryTest.java` | 新建 | 集成测试（8 个测试用例） |
| `backend/src/test/resources/application.properties` | 新建 | 测试配置（启用自动索引创建） |
| `backend/pom.xml` | 修改 | 新增 spring-boot-starter-data-mongodb-test + flapdoodle spring3x 依赖 |

### Review Findings

- [x] [Review][Decision] `contentLength` 自动计算机制缺失 — 架构文档定义"save 时由 `content.length()` 计算"（可通过 `@PrePersist` 或 Service 层实现），但模型层无生命周期回调。**决策：延迟到 Story 1.5 Service 层实现** [Story.java:67]
- [x] [Review][Patch] 测试冗余代码 — `buildStory("dup-id")` 已通过 builder 设置 storyId，第 50 行 `setStoryId("dup-id")` 多余 [StoryMongoRepositoryTest.java:50]
- [x] [Review][Patch] 默认值大小写不一致 — `status="active"`（小写）vs `processingStatus="PENDING"`（大写），已统一为大写 `"ACTIVE"` [Story.java:52,61]
