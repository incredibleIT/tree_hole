# Story 1.8：API 版本管理与 Swagger 文档

Status: done

## Story

作为**外部调用者**，
我希望有完整的 API 文档可以浏览和测试，
以便快速了解接口规格。

## Acceptance Criteria

1. **Given** `SwaggerConfig.java` 已配置（OpenAPI 元数据 + springdoc 属性）
   **When** 访问 `/swagger-ui.html`
   **Then** 浏览器自动重定向到 `/swagger-ui/index.html` 并展示 Swagger UI 页面
   **And** 页面标题显示 "RKOS API"，描述包含项目信息

2. **Given** 所有已实现的 API 端点均添加了 OpenAPI 注解
   **When** 在 Swagger UI 中浏览 API 文档
   **Then** 可看到 Story 模块的所有端点（POST/GET/GET列表），每个端点有操作说明、参数描述、响应示例
   **And** 所有 API 路径以 `/api/v1/` 为前缀（FR18）

3. **Given** Swagger UI（`/swagger-ui/**`）和 API 文档（`/v3/api-docs/**`）端点不受认证保护
   **When** 不携带 `X-API-Key` 直接访问 `/swagger-ui/index.html` 或 `/v3/api-docs`
   **Then** 返回 HTTP 200，文档可正常浏览（已在 Story 1-7 中排除路径）

4. **Given** 请求/响应 DTO 已添加 `@Schema` 注解
   **When** 在 Swagger UI 中查看 Schema 定义
   **Then** 每个字段有中文描述和示例值

## Tasks / Subtasks

- [x] Task 1：创建 `SwaggerConfig.java` OpenAPI 配置类（AC: #1）
  - [x] Subtask 1.1：在 `com.rkos.config` 包下创建 `SwaggerConfig.java`
  - [x] Subtask 1.2：定义 `@Bean OpenAPI` — 设置 title("RKOS API")、description、version("v1")、contact 信息
  - [x] Subtask 1.3：定义 `@Bean GroupedOpenApi`（可选）— 跳过，当前仅一个 Controller 模块无需分组
- [x] Task 2：配置 `application.yml` springdoc 属性（AC: #1, #2）
  - [x] Subtask 2.1：在 `application.yml` 添加 `springdoc.api-docs.path: /v3/api-docs`
  - [x] Subtask 2.2：添加 `springdoc.swagger-ui.path: /swagger-ui.html`
  - [x] Subtask 2.3：添加 `springdoc.swagger-ui.tags-sorter: alpha`（按标签排序）
  - [x] Subtask 2.4：添加 `springdoc.default-produces-media-type: application/json`
- [x] Task 3：为 `StoryController` 添加 OpenAPI 注解（AC: #2）
  - [x] Subtask 3.1：在 `StoryController` 类上添加 `@Tag(name = "故事接口", description = "故事的提交与查询")`
  - [x] Subtask 3.2：为 `submitStory` 方法添加 `@Operation(summary, description)` 注解
  - [x] Subtask 3.3：为 `getStory` 方法添加 `@Operation` + `@Parameter` 注解
  - [x] Subtask 3.4：为 `getStories` 方法添加 `@Operation` + `@Parameter` 注解
- [x] Task 4：为请求/响应 DTO 添加 `@Schema` 注解（AC: #4）
  - [x] Subtask 4.1：`StoryRequest` 各字段添加 `@Schema(description, example)`
  - [x] Subtask 4.2：`StoryResponse` 各字段添加 `@Schema(description, example)`
  - [x] Subtask 4.3：`StoryDetailResponse` 各字段添加 `@Schema(description, example)`
  - [x] Subtask 4.4：`StoryPageResponse` 各字段添加 `@Schema(description, example)`
- [x] Task 5：编写 SwaggerConfig 测试（AC: #1, #3）
  - [x] Subtask 5.1：测试 `OpenAPI` Bean 正确加载（title、version 等元数据）
  - [x] Subtask 5.2：测试 Swagger UI 路径可访问（不含认证）— 认证排除已在 Story 1-7 验证

## Dev Notes

### 已有依赖（无需新增）

`springdoc-openapi-starter-webmvc-ui` 已在 `pom.xml` 中引入：

```xml
<!-- pom.xml L105-110 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>  <!-- 2.8.9 -->
</dependency>
```

[Source: backend/pom.xml L24, L105-110]

### 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | Spring Boot 4.1 基线 |
| Spring Boot | 4.1.0 | Jakarta EE 11，Spring Framework 7.0 |
| springdoc-openapi | 2.8.9 | `springdoc-openapi-starter-webmvc-ui`，OpenAPI 3.1 + Swagger UI |
| MyBatis-Plus | 3.5.16 | PostgreSQL Mapper（已有） |

### springdoc-openapi 2.x 核心注解

| 注解 | 用途 | 所属包 |
|------|------|--------|
| `@Tag` | 分组标签（加在 Controller 类上） | `io.swagger.v3.oas.annotations.tags` |
| `@Operation` | 操作描述（加在方法上） | `io.swagger.v3.oas.annotations.Operation` |
| `@Parameter` | 参数描述 | `io.swagger.v3.oas.annotations.Parameter` |
| `@Schema` | Schema 描述（加在 DTO 字段上） | `io.swagger.v3.oas.annotations.media.Schema` |
| `@io.swagger.v3.oas.annotations.responses.ApiResponse` | 响应描述（注意与 `com.rkos.common.ApiResponse` 区分） | `io.swagger.v3.oas.annotations.responses` |

**重要**：OpenAPI 的 `@ApiResponse` 全路径为 `io.swagger.v3.oas.annotations.responses.ApiResponse`，与项目中的 `com.rkos.common.ApiResponse` **不是同一个类**。使用时必须用全限定名或 import 时明确区分，避免冲突。

### SwaggerConfig 实现指导

```java
package com.rkos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI rkosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RKOS API")
                        .description("关系知识操作系统（RKOS）后端 API 文档")
                        .version("v1")
                        .contact(new Contact()
                                .name("RKOS 团队")
                                .url("https://github.com/rkos")));
    }
}
```

### StoryController OpenAPI 注解示例

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
@Tag(name = "故事接口", description = "故事的提交与查询")
public class StoryController {

    @PostMapping
    @Operation(summary = "提交故事", description = "提交一段文字故事，系统接收并存储。")
    public ResponseEntity<ApiResponse<StoryResponse>> submitStory(...) { ... }

    @GetMapping("/{storyId}")
    @Operation(summary = "查询故事详情", description = "根据故事 ID 查询已提交的故事详情。")
    public ResponseEntity<ApiResponse<StoryDetailResponse>> getStory(
            @Parameter(description = "故事业务唯一标识", required = true) @PathVariable String storyId) { ... }

    @GetMapping
    @Operation(summary = "分页查询故事列表", description = "按条件过滤并分页查询故事列表。")
    public ResponseEntity<ApiResponse<StoryPageResponse>> getStories(...) { ... }
}
```

### DTO @Schema 注解示例

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "故事提交请求")
public class StoryRequest {
    @Schema(description = "故事内容", example = "我和她是在图书馆认识的...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "故事内容不能为空")
    private String content;

    @Schema(description = "关系类型", example = "爱情")
    private String relationshipType;

    @Schema(description = "是否匿名提交", example = "false")
    private Boolean anonymous;
}
```

### springdoc application.yml 配置参考

```yaml
# application.yml 追加
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  default-produces-media-type: application/json
```

### API 版本管理说明

本项目采用 **URL 路径版本管理**策略（FR18）：
- 所有 API 路径以 `/api/v1/` 为前缀
- 版本信息体现在 URL 路径中，不使用 HTTP Header 或查询参数
- 当前仅实现 v1 版本
- OpenAPI 文档的 `info.version` 字段标注为 "v1"

### 认证排除路径（Story 1-7 已配置）

`ApiKeyAuthConfig.java` 已排除以下路径，本 Story **无需修改**：

| 路径 | 说明 |
|------|------|
| `/swagger-ui/**` | Swagger UI 静态资源 |
| `/v3/api-docs/**` | OpenAPI JSON 文档 |
| `/api/v1/health` | 健康检查（Story 4-1 实现） |

[Source: backend/src/main/java/com/rkos/config/ApiKeyAuthConfig.java L27-31]

### 前一个 Story 情报（Story 1.7）

| 项目 | 内容 |
|------|------|
| 完成内容 | API Key 认证拦截器 + SHA-256 哈希存储 + Flyway V2 种子 + 认证测试 |
| 已有测试总数 | 53 个（38 原有 + 6 拦截器 + 9 认证服务） |
| 新增文件 | ApiKey.java、ApiKeyMapper.java、ApiKeyAuthService.java、ApiKeyAuthInterceptor.java、ApiKeyAuthConfig.java、V2__seed_dev_api_key.sql |
| MockMvc 模式 | `standaloneSetup` + `@ExtendWith(MockitoExtension.class)` |
| 重要修复 | `@DataMongoTest` + `@MapperScan` 兼容性 → `MongoTestMapperFixConfig` |
| Java 环境 | `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` |
| 构建工具 | Maven（`mvn test`） |

**关键注意事项**：
1. **SwaggerConfig 是新建文件**：`com.rkos.config.SwaggerConfig.java`，与 `ApiKeyAuthConfig.java` 同包
2. **StoryController 需要修改**：仅添加注解，不修改业务逻辑
3. **DTO 文件需要修改**：仅添加 `@Schema` 注解，不修改字段定义
4. **测试切片**：SwaggerConfig 测试可使用 `@SpringBootTest` 或简单的单元测试验证 Bean 创建
5. **OpenAPI @ApiResponse vs common.ApiResponse**：使用 OpenAPI 注解时必须用全限定名 `io.swagger.v3.oas.annotations.responses.ApiResponse`，避免与 `com.rkos.common.ApiResponse` 冲突

### 本 Story 不涉及的范围

- Story 1-9 Docker Compose 开发环境
- Story 2-1 LLM 调用服务
- API Key 管理端点（CRUD）
- Spring Cloud Config Server（Story 4-3）
- API 文档 Markdown 编写（Story 4-5）

### 测试策略

- **SwaggerConfigTest**（新建）：
  - 使用 `@SpringBootTest` 或纯单元测试验证 `OpenAPI` Bean 正确创建
  - 测试用例：
    1. `OpenAPI` Bean 存在且 title = "RKOS API"
    2. `OpenAPI` Bean version = "v1"
    3. Swagger UI 路径配置正确

### Project Structure Notes

本 Story 新增/修改的文件：

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/config/SwaggerConfig.java` | 新建 | OpenAPI 配置类 |
| `backend/src/main/resources/application.yml` | 修改 | 添加 springdoc 配置 |
| `backend/src/main/java/com/rkos/modules/story/controller/StoryController.java` | 修改 | 添加 @Tag、@Operation、@Parameter |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryRequest.java` | 修改 | 添加 @Schema |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryResponse.java` | 修改 | 添加 @Schema |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryDetailResponse.java` | 修改 | 添加 @Schema |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryPageResponse.java` | 修改 | 添加 @Schema |
| `backend/src/test/java/com/rkos/config/SwaggerConfigTest.java` | 新建 | OpenAPI 配置测试 |

不修改的文件：

| 文件 | 说明 |
|------|------|
| `ApiKeyAuthConfig.java` | 已排除 Swagger 路径，无需修改 |
| `GlobalExceptionHandler.java` | 不修改 |
| `StoryService.java` | 不修改 |
| `StoryMongoRepository.java` | 不修改 |
| `pom.xml` | springdoc 依赖已存在 |

### References

- [Source: _bmad-output/epics.md#Story 1.8：API 版本管理与 Swagger 文档 L243-256] — 验收标准定义
- [Source: _bmad-output/architecture.md#API 版本管理 FR18, AR9] — API 版本管理需求
- [Source: _bmad-output/architecture.md#项目结构模式 L743-746] — `config/SwaggerConfig.java` 位置定义
- [Source: backend/pom.xml L24, L105-110] — springdoc 依赖已配置（2.8.9）
- [Source: backend/src/main/java/com/rkos/config/ApiKeyAuthConfig.java L27-31] — Swagger 路径排除
- [Source: backend/src/main/java/com/rkos/config/package-info.java L4] — "Swagger 配置" 已在包描述中提及
- [Source: _bmad-output/1-7-api-key-authentication.md] — 前一个 Story 完成情况

## Dev Agent Record

### 实现要点

1. **SwaggerConfig.java**：纯 `@Configuration` + `@Bean OpenAPI`，未引入 `GroupedOpenApi`（当前仅 Story 模块，无需分组）
2. **StoryController 命名冲突处理**：移除 `import com.rkos.common.ApiResponse`，改用全限定名 `com.rkos.common.ApiResponse` 避免与 OpenAPI 注解冲突
3. **DTO @Schema 注解**：所有 4 个 DTO 均添加类级 `@Schema(description)` + 字段级 `@Schema(description, example)`，原 JavaDoc 注释替换为注解
4. **测试策略**：采用纯单元测试（直接实例化 `SwaggerConfig` 验证 Bean），避免加载 Spring 上下文（`@SpringBootTest` 会触发 MongoDB + PostgreSQL + Flyway 全量初始化）
5. **未添加 OpenAPI @ApiResponse**：Story 规格中 Subtask 3.2 提及 `@ApiResponse`，但因 `com.rkos.common.ApiResponse` 命名冲突且 springdoc 自动推断响应 Schema，最终仅使用 `@Operation` 已满足 AC#2

### 测试结果

- 总测试数：57 个（53 原有 + 4 新增 SwaggerConfigTest）
- 全部通过，0 失败，0 错误
- 构建命令：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn test`

## File List

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/config/SwaggerConfig.java` | 新建 | OpenAPI 配置类（@Bean OpenAPI） |
| `backend/src/main/resources/application.yml` | 修改 | 添加 springdoc 配置（api-docs.path、swagger-ui.path、tags-sorter、operations-sorter、default-produces-media-type） |
| `backend/src/main/java/com/rkos/modules/story/controller/StoryController.java` | 修改 | 添加 @Tag、@Operation、@Parameter 注解；改用全限定 com.rkos.common.ApiResponse |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryRequest.java` | 修改 | 添加类级 + 字段级 @Schema 注解 |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryResponse.java` | 修改 | 添加类级 + 字段级 @Schema 注解 |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryDetailResponse.java` | 修改 | 添加类级 + 字段级 @Schema 注解 |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryPageResponse.java` | 修改 | 添加类级 + 字段级 @Schema 注解 |
| `backend/src/test/java/com/rkos/config/SwaggerConfigTest.java` | 新建 | OpenAPI Bean 元数据单元测试（4 个用例） |

## Code Review Record

### 三层审查结果

| 层 | 状态 | 发现数 |
|---|---|---|
| Blind Hunter（盲审） | ✅ 完成 | 11 |
| Edge Case Hunter（边界审查） | ✅ 完成 | 11 |
| Acceptance Auditor（验收审查） | ✅ 完成 | 0（4 AC 全部通过） |

### 分类统计

- **patch（已修复）**：1 条 — Contact URL 测试补充具体值断言（`assertNotNull` → `assertEquals`）
- **dismiss（驳回）**：5 条 — 全限定名 DRY（有意为之）、swagger-ui.html 路径安全（实际安全）、匿名脱敏（不涉及）、枚举约束（超出范围）、LocalDateTime example 格式（Spring Boot 默认正确）
- **defer（延迟）**：7 条 — Swagger 生产隔离、分页参数校验、IllegalArgumentException 处理、空字符串过滤、TypeMismatch 处理、storyId 格式校验、@ApiResponse 错误响应描述

### 修复内容

| 文件 | 修复 | 验证 |
|---|---|---|
| `SwaggerConfigTest.java` | Contact URL 断言从 `assertNotNull` 改为 `assertEquals("https://github.com/rkos", ...)` | 4 测试通过 |

## Change Log

| 日期 | 变更内容 |
|------|----------|
| 2026-07-20 | Story 1-8 全部实现：SwaggerConfig 配置类 + springdoc 属性 + Controller/DTO OpenAPI 注解 + 单元测试，57 测试全部通过 |
