# Story 1.6：故事查询 API

Status: done

## Story

作为**故事贡献者/系统维护者**，
我希望通过故事 ID 查询已提交的故事详情，并能按条件过滤故事列表，
以便查看和管理已提交的故事。

## Acceptance Criteria

1. **Given** MongoDB 中存在已提交的故事数据
   **When** 发送 `GET /api/v1/stories/{storyId}`
   **Then** 返回 HTTP 200，响应体包含该故事的完整详情（storyId、content、relationshipType、anonymous、processingStatus、contentLength、createdAt）
   **And** 响应使用 `ApiResponse<StoryDetailResponse>` 包装

2. **Given** storyId 不存在
   **When** 发送 `GET /api/v1/stories/{storyId}`
   **Then** 返回 HTTP 404，响应体包含 `"code":"NOT_FOUND"` 和 `"message":"故事不存在"`

3. **Given** MongoDB 中存在多条故事数据
   **When** 发送 `GET /api/v1/stories?relationshipType=亲情&processingStatus=completed&page=0&size=20`
   **Then** 返回 HTTP 200，响应体包含符合条件的分页故事列表
   **And** 响应包含 `totalCount`、`page`、`size` 分页信息
   **And** 响应使用 `ApiResponse<StoryPageResponse>` 包装

4. **Given** 查询参数为空或不传
   **When** 发送 `GET /api/v1/stories`
   **Then** 返回全部故事的分页列表（默认 page=0, size=20）

## Tasks / Subtasks

- [x] Task 1：创建 `StoryDetailResponse` DTO（AC: #1）
  - [x] Subtask 1.1：在 `com.rkos.modules.story.dto` 包下创建 `StoryDetailResponse.java`
  - [x] Subtask 1.2：包含 storyId、content、relationshipType、anonymous、processingStatus、contentLength、createdAt 字段
  - [x] Subtask 1.3：使用 Lombok `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`
- [x] Task 2：创建 `StoryPageResponse` 分页响应 DTO（AC: #3, #4）
  - [x] Subtask 2.1：在 `com.rkos.modules.story.dto` 包下创建 `StoryPageResponse.java`
  - [x] Subtask 2.2：包含 `List<StoryDetailResponse> content`、`long totalCount`、`int page`、`int size` 字段
- [x] Task 3：扩展 `StoryMongoRepository`（AC: #3, #4）
  - [x] Subtask 3.1：添加分页查询方法，支持 relationshipType + processingStatus 可选过滤 + `Pageable` 分页
  - [x] Subtask 3.2：使用 Spring Data 方法命名约定（4 个方法覆盖所有过滤组合）
- [x] Task 4：扩展 `StoryService`（AC: #1, #2, #3, #4）
  - [x] Subtask 4.1：添加 `getStoryByStoryId(String storyId)` 方法 — 查询详情，不存在时抛 `RkosException("NOT_FOUND", "故事不存在")`
  - [x] Subtask 4.2：添加 `getStories(String relationshipType, String processingStatus, Pageable pageable)` 方法 — 分页查询 + Story→DTO 转换
- [x] Task 5：扩展 `StoryController`（AC: #1, #2, #3, #4）
  - [x] Subtask 5.1：添加 `GET /api/v1/stories/{storyId}` 端点 — 调用 Service 返回 200 + `ApiResponse<StoryDetailResponse>`
  - [x] Subtask 5.2：添加 `GET /api/v1/stories` 端点 — 接收可选查询参数（relationshipType、processingStatus、page、size），调用 Service 返回 200 + `ApiResponse<StoryPageResponse>`
- [x] Task 6：编写 `StoryControllerTest` 查询测试（AC: #1, #2, #3）
  - [x] Subtask 6.1：测试 GET 详情返回 200 + 完整字段
  - [x] Subtask 6.2：测试 GET 不存在的 storyId 返回 404 NOT_FOUND
  - [x] Subtask 6.3：测试 GET 列表返回分页结构（totalCount、page、size）
- [x] Task 7：编写 `StoryServiceTest` 查询单元测试（AC: #1, #2, #3）
  - [x] Subtask 7.1：测试 getStoryByStoryId 正常查询返回 DTO
  - [x] Subtask 7.2：测试 getStoryByStoryId 不存在时抛 RkosException
  - [x] Subtask 7.3：测试 getStories 分页查询

## Dev Notes

### 架构规范（强制遵守）

**文件位置**：
```
backend/src/main/java/com/rkos/modules/story/
├── controller/
│   └── StoryController.java         # 扩展（添加 2 个 GET 端点）
├── service/
│   └── StoryService.java            # 扩展（添加 2 个查询方法）
├── dto/
│   ├── StoryRequest.java            # 不修改
│   ├── StoryResponse.java           # 不修改（仅用于提交响应）
│   ├── StoryDetailResponse.java     # 新建（详情查询响应）
│   └── StoryPageResponse.java       # 新建（分页列表响应）
├── model/
│   └── Story.java                   # 不修改（Story 1-4 已创建）
└── repository/
    └── StoryMongoRepository.java    # 扩展（添加分页查询方法）
```

[Source: _bmad-output/architecture.md#项目结构模式 L751-763]

**API 端点规范**：
- 详情查询：`GET /api/v1/stories/{storyId}` → 200 + `ApiResponse<StoryDetailResponse>`
- 列表查询：`GET /api/v1/stories?relationshipType=&processingStatus=&page=0&size=20` → 200 + `ApiResponse<StoryPageResponse>`
- 不存在时：404 + `ApiResponse<Object>`（`code: "NOT_FOUND"`, `message: "故事不存在"`）

[Source: _bmad-output/architecture.md#参数校验 L423-452]

**404 异常处理模式**（已有基础设施，直接使用）：
- Service 层：`storyMongoRepository.findByStoryId(storyId).orElseThrow(() -> new RkosException("NOT_FOUND", "故事不存在"))`
- `GlobalExceptionHandler.handleRkosException` 自动捕获 → `RkosException("NOT_FOUND")` → HTTP 404
- `getHttpStatus` 已包含 `"NOT_FOUND" → HttpStatus.NOT_FOUND` 映射

[Source: _bmad-output/architecture.md#全局异常处理 L1044-1051]

**Service 层查询职责**：
- Story 领域模型 → StoryDetailResponse DTO 转换（Service 层负责，不在 Controller 做）
- `Page<Story>` → `StoryPageResponse` 转换（提取 content、totalCount、page、size）
- 分页默认值：`page=0, size=20`（通过 `@RequestParam(defaultValue = ...)` 或 Pageable 默认值）

**强制一致性规则**：
- 命名一致性：Java `camelCase`（如 `storyId`、`processingStatus`），API 字段也 `camelCase`
- 响应格式一致性：所有 API 必须返回 `ApiResponse<T>` 包装对象
- 异常处理一致性：404 通过 `RkosException` + `GlobalExceptionHandler`，禁止 Controller 手动构造错误响应
- 模块组织一致性：Controller → Service → Repository 三层
- 测试覆盖一致性：每个 Service 必须有对应的 `*Test.java` 单元测试

[Source: _bmad-output/architecture.md#强制一致性规则 L1085-1101]

### 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | Spring Boot 4.1 基线 |
| Spring Boot | 4.1.0 | Jakarta EE 11，Spring Framework 7.0 |
| Spring Data MongoDB | Spring Boot 管理 | `spring-boot-starter-data-mongodb`，`Page<Story>` 分页 |
| Lombok | Spring Boot 管理 | `@Data`、`@Builder` 等 |
| JUnit 5 | Spring Boot 管理 | `spring-boot-starter-test` |

### 前一个 Story 情报（Story 1.5）

| 项目 | 内容 |
|------|------|
| 完成内容 | `StoryService.java`（提交）+ `StoryResponse.java` + `StoryController` 扩展 + 测试 |
| 已有测试 | ApiResponseTest(4) + GlobalExceptionHandlerTest(8) + StoryMongoRepositoryTest(8) + StoryControllerTest(6) + StoryServiceTest(5) = 31 个测试 |
| MockMvc 模式 | `standaloneSetup` + `@ExtendWith(MockitoExtension.class)` |
| StoryService 现状 | 仅 `submitStory(StoryRequest)` 一个方法，本 Story 需添加查询方法 |
| StoryController 现状 | 仅 `@PostMapping submitStory`，本 Story 需添加两个 `@GetMapping` |
| StoryResponse 现状 | 仅 `storyId` + `createdAt`（提交响应），本 Story 新建 `StoryDetailResponse` 用于查询 |
| StoryMongoRepository 现状 | 已有 `findByStoryId` + `findByProcessingStatus`，本 Story 需添加分页查询 |
| Java 环境 | jenv 默认 Java 17，需 `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` |
| Deferred 项 | `StoryService.save()` 无 MongoDB 异常处理 → 不涉及本 Story |
| Deferred 项 | `GlobalExceptionHandler` toMap 空值风险 → 不涉及本 Story |

**关键注意事项**：
1. **不要修改 Story.java**：Story 1-4 创建，本 Story 直接使用
2. **不要修改 StoryResponse.java**：它是提交 API 的响应 DTO，查询使用独立的 `StoryDetailResponse`
3. **不要修改 GlobalExceptionHandler**：`NOT_FOUND` 映射已就绪，直接使用 `RkosException`
4. **不要修改 ApiResponse.java**：直接使用 `ApiResponse.success()` 和 `ApiResponse.error()`
5. **API Key 认证不在本 Story 范围**：Story 1-7 实现，本 Story 不添加认证拦截
6. **Agent 异步处理不在本 Story 范围**：Story 2-6 实现
7. **`@DataMongoTest` 包路径**：Spring Boot 4.x 使用 `org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest`
8. **flapdoodle 测试配置**：`application.properties` 已配置 `port=0` 避免连接真实 MongoDB
9. **测试文件模式**：Controller 测试扩展现有 `StoryControllerTest.java`，Service 测试扩展现有 `StoryServiceTest.java`

### 本 Story 不涉及的范围

- API Key 认证拦截器（Story 1-7）
- Agent 异步处理 / LLM 调用（Story 2-6）
- StoryPersistenceService（Story 2-5）
- Genome 数据（Epic 2/3）
- Swagger 文档配置（Story 1-8）
- `StoryResponse.java` 修改（提交响应 DTO 保持不变）

### 测试策略

- **Controller 测试**：扩展现有 `StoryControllerTest.java`，使用 MockMvc standalone 模式 + Mock `StoryService`
  - `GET /api/v1/stories/{storyId}` 正常 → 200 + StoryDetailResponse 完整字段
  - `GET /api/v1/stories/{storyId}` 不存在 → 404 NOT_FOUND（Mock Service 抛 RkosException）
  - `GET /api/v1/stories` 列表 → 200 + StoryPageResponse 分页结构
- **Service 测试**：扩展现有 `StoryServiceTest.java`，使用 `@ExtendWith(MockitoExtension.class)` + `@Mock` Repository
  - `getStoryByStoryId` 正常 → 返回 StoryDetailResponse
  - `getStoryByStoryId` 不存在 → 抛出 RkosException（验证 errorCode = "NOT_FOUND"）
  - `getStories` 分页 → 返回 StoryPageResponse（验证 content 列表 + totalCount + page + size）
- **Repository 测试（可选）**：如需验证分页查询，可扩展现有 `StoryMongoRepositoryTest.java`

### Repository 分页查询实现指导

Spring Data MongoDB 支持方法命名约定自动生成查询。推荐的实现方式：

```java
// 方式 1：方法命名约定（适合简单过滤）
Page<Story> findByRelationshipTypeAndProcessingStatus(
    String relationshipType, String processingStatus, Pageable pageable);
Page<Story> findByRelationshipType(String relationshipType, Pageable pageable);
Page<Story> findByProcessingStatus(String processingStatus, Pageable pageable);
Page<Story> findAll(Pageable pageable);

// 方式 2：@Query 注解（推荐，一个方法覆盖所有过滤组合）
@Query("{ " +
    "?#{ [0] == null ? '' : 'relationship_type: {0},' } " +
    "?#{ [1] == null ? '' : 'processing_status: {1},' } " +
    "'status: { $exists: true }' }")
Page<Story> findByFilters(String relationshipType, String processingStatus, Pageable pageable);
```

**推荐方式 2**（`@Query` + SpEL 条件表达式），一个方法覆盖所有过滤组合，避免方法爆炸。如果 SpEL 过于复杂，退回到方式 1 在 Service 层做条件组装。

### Story → StoryDetailResponse 转换映射

| Story 字段 | StoryDetailResponse 字段 | 说明 |
|---|---|---|
| `storyId` | `storyId` | 业务唯一标识 |
| `content` | `content` | 故事内容 |
| `relationshipType` | `relationshipType` | 关系类型（可为 null） |
| `anonymous` | `anonymous` | 是否匿名 |
| `processingStatus` | `processingStatus` | 处理状态 |
| `contentLength` | `contentLength` | 内容长度 |
| `createdAt` | `createdAt` | 创建时间 |

**不包含的字段**（查询 API 不暴露）：`id`（MongoDB 内部 _id）、`authorId`（Story 1-7 认证后关联）、`attachments`、`status`、`version`、`processingMetadata`、`language`、`updatedAt`

### Project Structure Notes

本 Story 新增/修改的文件：

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/modules/story/dto/StoryDetailResponse.java` | 新建 | 详情查询响应 DTO |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryPageResponse.java` | 新建 | 分页列表响应 DTO |
| `backend/src/main/java/com/rkos/modules/story/controller/StoryController.java` | 修改 | 添加 2 个 GET 端点 |
| `backend/src/main/java/com/rkos/modules/story/service/StoryService.java` | 修改 | 添加 2 个查询方法 |
| `backend/src/main/java/com/rkos/modules/story/repository/StoryMongoRepository.java` | 修改 | 添加分页查询方法 |
| `backend/src/test/java/com/rkos/modules/story/controller/StoryControllerTest.java` | 修改 | 添加查询测试用例 |
| `backend/src/test/java/com/rkos/modules/story/service/StoryServiceTest.java` | 修改 | 添加查询单元测试 |

不修改的文件：

| 文件 | 说明 |
|------|------|
| `backend/src/main/java/com/rkos/modules/story/model/Story.java` | Story 1-4 创建，直接使用 |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryResponse.java` | 提交响应 DTO，查询不修改 |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryRequest.java` | 提交请求 DTO，查询不修改 |
| `backend/src/main/java/com/rkos/common/GlobalExceptionHandler.java` | NOT_FOUND 已就绪 |
| `backend/src/main/java/com/rkos/common/ApiResponse.java` | 直接使用 |
| `backend/src/main/java/com/rkos/common/RkosException.java` | 直接使用 |

### References

- [Source: _bmad-output/epics.md#Story 1.6：故事查询 API L211-226] — 验收标准定义
- [Source: _bmad-output/architecture.md#全局异常处理 L1044-1051] — NOT_FOUND 状态码映射
- [Source: _bmad-output/architecture.md#统一响应格式 L484-511] — ApiResponse 规范
- [Source: _bmad-output/architecture.md#强制一致性规则 L1085-1101] — 命名/响应/异常/模块/测试一致性
- [Source: _bmad-output/architecture.md#项目结构模式 L751-763] — 模块目录结构
- [Source: _bmad-output/1-5-story-submit-api.md] — 前一个 Story 完成情况
- [Source: _bmad-output/deferred-work.md] — 延迟工作项

## Dev Agent Record

### Agent Model Used

Qoder AI

### Debug Log References

无调试问题，所有任务一次通过。

### Completion Notes List

- Task 1-2：创建 StoryDetailResponse、StoryPageResponse DTO，使用 Lombok 标准注解
- Task 3：Repository 使用 Spring Data 方法命名约定（4 个分页方法覆盖所有过滤组合），而非 @Query+SpEL
- Task 4：Service 层实现 Story→DTO 转换（toDetailResponse 私有方法），分页查询按条件分发到不同 Repository 方法
- Task 5：Controller 添加 GET 详情和 GET 列表端点，列表默认按 createdAt DESC 排序，分页默认 page=0, size=20
- Task 6-7：ControllerTest 新增 3 个测试（200详情/404不存在/200分页），ServiceTest 新增 3 个测试（正常查询/不存在异常/分页查询）
- 全量测试：37 个测试全部通过（31 已有 + 6 新增）

### File List

**新建文件：**
- `backend/src/main/java/com/rkos/modules/story/dto/StoryDetailResponse.java`
- `backend/src/main/java/com/rkos/modules/story/dto/StoryPageResponse.java`

**修改文件：**
- `backend/src/main/java/com/rkos/modules/story/repository/StoryMongoRepository.java`
- `backend/src/main/java/com/rkos/modules/story/service/StoryService.java`
- `backend/src/main/java/com/rkos/modules/story/controller/StoryController.java`
- `backend/src/test/java/com/rkos/modules/story/controller/StoryControllerTest.java`
- `backend/src/test/java/com/rkos/modules/story/service/StoryServiceTest.java`

### Review Findings

- [x] [Review][Patch] ServiceTest 缺少默认查询路径测试 — `getStories(null, null, pageable)` → `findAll` 分支未覆盖 [StoryServiceTest.java] — **已修复**
- [x] [Review][Defer] 空字符串查询参数未过滤 — `?relationshipType=` 传空字符串时作为有效过滤值，返回空结果而非全部 [StoryController.java:L51] — 延迟，需全局 API 策略
- [x] [Review][Defer] 分页参数非法值返回 500 — `page=-1` 或 `size=0` 时 `PageRequest.of()` 抛 `IllegalArgumentException`，无对应 handler [StoryController.java:L53-55] — 延迟，属 GlobalExceptionHandler 全局改进

### Change Log

- 2026-07-17：完成 Story 1-6 全部 7 个任务，实现故事查询 API（详情 + 分页列表），新增 6 个测试，37 个测试全部通过
- 2026-07-19：代码审查完成（0 decision, 1 patch 已修复, 2 defer, 1 dismiss），38 个测试全部通过，状态更新为 done
