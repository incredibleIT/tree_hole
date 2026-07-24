# Story 1.5：故事提交 API

Status: done

## Story

作为**故事贡献者**，
我希望通过 API 提交一段文字故事，
以便系统接收并存储我的故事内容。

## Acceptance Criteria

1. **Given** 发送 `POST /api/v1/stories` 请求，Body 包含 content（必填）、relationship_type、anonymous 字段
   **When** 参数校验通过
   **Then** 返回 HTTP 201，响应体包含 story_id 和 created_at
   **And** 故事数据已写入 MongoDB `stories` 集合
   **And** `content_length` 由 Service 层自动计算（`content.length()`）
   **And** `processing_status` 默认为 `"PENDING"`

2. **Given** content 为空或空白
   **When** 发送 `POST /api/v1/stories` 请求
   **Then** 返回 HTTP 400 参数校验错误

3. **Given** content 超过 10000 字
   **When** 发送 `POST /api/v1/stories` 请求
   **Then** 返回 HTTP 400 参数校验错误

4. **Given** 响应时间要求 ≤ 2 秒（NFR1，不含 LLM 处理）
   **When** 故事提交成功
   **Then** API 同步返回 201，无阻塞操作

## Tasks / Subtasks

- [x] Task 1：扩展 `StoryRequest` DTO（AC: #1, #2, #3）
  - [x] Subtask 1.1：添加 `relationshipType` 字段（可选，String）
  - [x] Subtask 1.2：添加 `anonymous` 字段（可选，Boolean，默认 false）
  - [x] Subtask 1.3：保留 `content` 字段的 `@NotBlank` + `@Size(max = 10000)` 校验
- [x] Task 2：创建 `StoryResponse` DTO（AC: #1）
  - [x] Subtask 2.1：在 `com.rkos.modules.story.dto` 包下创建 `StoryResponse.java`
  - [x] Subtask 2.2：包含 `storyId`（String）和 `createdAt`（LocalDateTime）字段
  - [x] Subtask 2.3：使用 Lombok `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`
- [x] Task 3：创建 `StoryService` 服务层（AC: #1, #4）
  - [x] Subtask 3.1：在 `com.rkos.modules.story.service` 包下创建 `StoryService.java`
  - [x] Subtask 3.2：注入 `StoryMongoRepository`
  - [x] Subtask 3.3：实现 `submitStory(StoryRequest)` 方法：生成 UUID storyId、设置默认值、计算 contentLength、保存到 MongoDB
  - [x] Subtask 3.4：返回 `StoryResponse`（包含 storyId 和 createdAt）
- [x] Task 4：更新 `StoryController`（AC: #1, #2）
  - [x] Subtask 4.1：注入 `StoryService`
  - [x] Subtask 4.2：`submitStory` 方法调用 `storyService.submitStory(request)` 并返回 201 + `ApiResponse<StoryResponse>`
- [x] Task 5：编写 Controller 集成测试（AC: #1, #2, #3）
  - [x] Subtask 5.1：创建 `StoryControllerTest.java`，使用 MockMvc + `@WebMvcTest` 或 standalone 模式
  - [x] Subtask 5.2：测试正常提交返回 201 + storyId + createdAt
  - [x] Subtask 5.3：测试 content 为空返回 400
  - [x] Subtask 5.4：测试 content 超长返回 400
  - [x] Subtask 5.5：测试响应格式符合 `ApiResponse<T>` 规范

## Dev Notes

### 架构规范（强制遵守）

**文件位置**：
```
backend/src/main/java/com/rkos/modules/story/
├── controller/
│   └── StoryController.java         # 扩展（桩实现 → 注入 Service）
├── service/
│   └── StoryService.java            # 新建
├── dto/
│   ├── StoryRequest.java            # 扩展（添加字段）
│   └── StoryResponse.java           # 新建
├── model/
│   └── Story.java                   # 不修改（Story 1-4 已创建）
└── repository/
    └── StoryMongoRepository.java    # 不修改（Story 1-4 已创建）
```

[Source: _bmad-output/architecture.md#项目结构模式 L751-763]

**API 端点规范**：
- 路径：`POST /api/v1/stories`（已有 `@RequestMapping("/api/v1/stories")`）
- 请求体：`@Valid @RequestBody StoryRequest`
- 成功响应：HTTP 201 + `ApiResponse<StoryResponse>`
- 校验失败响应：HTTP 400 + `ApiResponse<Map<String, String>>`（由 `GlobalExceptionHandler` 自动处理）

[Source: _bmad-output/architecture.md#参数校验 L423-452]

**Service 层职责**（架构文档正确示例参考）：
- 接收 DTO → 构建领域模型 → 保存到 Repository → 返回响应 DTO
- `contentLength` 自动计算：`story.setContentLength(request.getContent().length())`（Story 1-4 审查延迟项）
- `storyId` 使用 `UUID.randomUUID().toString()` 生成
- `createdAt` / `updatedAt` 使用 `LocalDateTime.now()` 设置
- `processingStatus` 默认为 `"PENDING"`（Story 模型已有 `@Builder.Default`）
- `status` 默认为 `"ACTIVE"`（Story 模型已有 `@Builder.Default`）
- `version` 默认为 `1`（Story 模型已有 `@Builder.Default`）

[Source: _bmad-output/architecture.md#正确示例 L1130-1155]

**统一响应格式**：
- 成功：`ApiResponse.success(storyResponse)` → `{"code":"SUCCESS","message":"操作成功","data":{...},"timestamp":"..."}`
- 校验失败：`GlobalExceptionHandler.handleValidationException` 自动捕获 → 400 + `{"code":"VALIDATION_ERROR","message":"参数校验失败","data":{...}}`

[Source: _bmad-output/architecture.md#统一响应格式 L484-511]

**强制一致性规则**：
- 命名一致性：Java `camelCase`（如 `storyId`），API 字段也 `camelCase`
- 响应格式一致性：所有 API 必须返回 `ApiResponse<T>` 包装对象
- 异常处理一致性：所有异常通过 `GlobalExceptionHandler` 处理，禁止在 Controller 中手动 catch
- 模块组织一致性：Controller → Service → Repository 三层
- 测试覆盖一致性：每个 Service 必须有对应的 `*Test.java` 单元测试

[Source: _bmad-output/architecture.md#强制一致性规则 L1085-1101]

### 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | Spring Boot 4.1 基线 |
| Spring Boot | 4.1.0 | Jakarta EE 11，Spring Framework 7.0 |
| Spring Data MongoDB | Spring Boot 管理 | `spring-boot-starter-data-mongodb` |
| Bean Validation | Jakarta Validation | `spring-boot-starter-validation` |
| Lombok | Spring Boot 管理 | `@Data`、`@Builder` 等 |
| JUnit 5 | Spring Boot 管理 | `spring-boot-starter-test` |

### 前一个 Story 情报（Story 1.4）

| 项目 | 内容 |
|------|------|
| 完成内容 | `Story.java` 领域模型 + `StoryMongoRepository.java` + 集成测试 |
| 已有测试 | ApiResponseTest(4) + GlobalExceptionHandlerTest(8) + StoryMongoRepositoryTest(8) = 20 个测试 |
| MockMvc 模式 | `standaloneSetup`，不加载 Spring 上下文 |
| Java 环境 | jenv 默认 Java 17，需 `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` |
| Deferred 项 | `contentLength` 自动计算 → **本 Story Service 层实现** |
| Deferred 项 | `ConstraintViolationException` / `MissingServletRequestPartException` → 不涉及本 Story |

**关键注意事项**：
1. **StoryController 和 StoryRequest 是桩实现**：Story 1-3 创建的桩文件，本 Story 需要扩展它们
2. **不要修改 Story.java 和 StoryMongoRepository.java**：它们由 Story 1-4 创建，本 Story 直接使用
3. **不要修改 GlobalExceptionHandler**：参数校验异常已由 `handleValidationException` 处理
4. **API Key 认证不在本 Story 范围**：Story 1-7 实现，本 Story 不添加认证拦截
5. **Agent 异步处理不在本 Story 范围**：Story 2-6 实现，本 Story 仅同步保存到 MongoDB
6. **`@DataMongoTest` 包路径已变更**：Spring Boot 4.x 使用 `org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest`
7. **flapdoodle 测试配置**：`application.properties` 已配置 `port=0` 避免连接真实 MongoDB

### 本 Story 不涉及的范围

- API Key 认证拦截器（Story 1-7）
- Agent 异步处理 / LLM 调用（Story 2-6）
- StoryPersistenceService（Story 2-5）
- 故事查询 API（Story 1-6）
- `@PrePersist` 回调（延迟决策：Service 层处理）

### 测试策略

- **Controller 测试**：使用 MockMvc standalone 模式（与 Story 1-3 的 `GlobalExceptionHandlerTest` 一致），Mock `StoryService`
- **Service 测试**：使用 `@ExtendWith(MockitoExtension.class)` + `@Mock` Repository + `@InjectMocks` Service
- **测试覆盖**：
  - 正常提交 → 201 + `ApiResponse<StoryResponse>`
  - content 为空 → 400 VALIDATION_ERROR
  - content 超长 → 400 VALIDATION_ERROR
  - Service 层 contentLength 自动计算验证
  - Service 层 storyId UUID 生成验证

### Project Structure Notes

本 Story 新增/修改的文件：

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/modules/story/service/StoryService.java` | 新建 | 故事提交服务 |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryResponse.java` | 新建 | 提交响应 DTO |
| `backend/src/main/java/com/rkos/modules/story/controller/StoryController.java` | 修改 | 桩实现 → 注入 Service |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryRequest.java` | 修改 | 添加 relationshipType + anonymous |
| `backend/src/test/java/com/rkos/modules/story/controller/StoryControllerTest.java` | 新建 | Controller 测试 |
| `backend/src/test/java/com/rkos/modules/story/service/StoryServiceTest.java` | 新建 | Service 单元测试 |

不修改的文件：

| 文件 | 说明 |
|------|------|
| `backend/src/main/java/com/rkos/modules/story/model/Story.java` | Story 1-4 创建，直接使用 |
| `backend/src/main/java/com/rkos/modules/story/repository/StoryMongoRepository.java` | Story 1-4 创建，直接使用 |
| `backend/src/main/java/com/rkos/common/GlobalExceptionHandler.java` | 已有校验异常处理，无需修改 |
| `backend/src/main/java/com/rkos/common/ApiResponse.java` | 直接使用 |

### References

- [Source: _bmad-output/epics.md#Story 1.5：故事提交 API L195-210] — 验收标准定义
- [Source: _bmad-output/architecture.md#参数校验 L423-452] — StoryRequest + StoryController 规范
- [Source: _bmad-output/architecture.md#正确示例 L1106-1155] — Controller + Service 正确示例
- [Source: _bmad-output/architecture.md#统一响应格式 L484-511] — ApiResponse 规范
- [Source: _bmad-output/architecture.md#强制一致性规则 L1085-1101] — 命名/响应/异常/模块/测试一致性
- [Source: _bmad-output/architecture.md#项目结构模式 L751-763] — 模块目录结构
- [Source: _bmad-output/1-4-story-domain-model-mongodb.md] — 前一个 Story 完成情况
- [Source: _bmad-output/deferred-work.md] — contentLength 自动计算延迟项

## Dev Agent Record

### Agent Model Used

Qoder AI

### Debug Log References

无调试问题，所有代码一次编译通过，31 个测试全部绿灯。

### Completion Notes List

- StoryService 通过 Builder 模式构建 Story 领域模型，contentLength 由 Service 层 `request.getContent().length()` 计算（承接 Story 1-4 延迟项）
- anonymous 字段 null 时默认 false，relationshipType 允许 null 透传
- StoryController 从桩实现升级为注入 StoryService，返回 HTTP 201 + `ApiResponse<StoryResponse>`
- GlobalExceptionHandlerTest 同步修复：StoryController 构造注入需要 mock StoryService，成功用例从 200 更新为 201
- StoryServiceTest 额外编写（规格文件 Task 5 仅要求 Controller 测试，但架构规范要求每个 Service 必须有对应单元测试）
- 测试总计：31 个（ApiResponseTest 4 + GlobalExceptionHandlerTest 8 + StoryMongoRepositoryTest 8 + StoryControllerTest 6 + StoryServiceTest 5）

### Review Findings

- [x] [Review][Patch] `StoryControllerTest` MockitoAnnotations.openMocks() 资源泄漏 — 已修复：改为 `@ExtendWith(MockitoExtension.class)`，删除手动 `openMocks()` [`StoryControllerTest.java:L43`]
- [x] [Review][Defer] `StoryService.save()` 无 MongoDB 异常语义处理 — deferred, MVP 阶段 GenericExceptionHandler 已兜底（500），后续可添加 `DataAccessException` 专用 handler [`StoryService.java:L44`]
- [x] [Review][Defer] `GlobalExceptionHandler` Collectors.toMap 空值风险 — deferred, 预先存在（Story 1-3），当前所有校验注解均设 message 属性，实际不触发 [`GlobalExceptionHandler.java:L37-41`]

### Change Log

- 2025-07-16：完成 Story 1-5 全部实现与测试
- 2025-07-16：代码审查完成（1 patch + 2 defer）

### File List

**新建文件：**
- `backend/src/main/java/com/rkos/modules/story/service/StoryService.java`
- `backend/src/main/java/com/rkos/modules/story/dto/StoryResponse.java`
- `backend/src/test/java/com/rkos/modules/story/controller/StoryControllerTest.java`
- `backend/src/test/java/com/rkos/modules/story/service/StoryServiceTest.java`

**修改文件：**
- `backend/src/main/java/com/rkos/modules/story/controller/StoryController.java`（桩实现 → 注入 Service + 201 响应）
- `backend/src/main/java/com/rkos/modules/story/dto/StoryRequest.java`（添加 relationshipType + anonymous）
- `backend/src/test/java/com/rkos/common/GlobalExceptionHandlerTest.java`（适配 StoryController 构造注入变更）
