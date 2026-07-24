# Story 1.3：统一响应格式与全局异常处理

Status: done

## Story

作为**API 调用者**，
我希望所有 API 返回统一的响应格式，
以便我能一致地解析成功和错误响应。

## Acceptance Criteria

1. **Given** `ApiResponse<T>` 已实现（包含 code、message、data、timestamp 字段）
   **When** 任何 API 返回成功或抛出异常
   **Then** 响应体统一使用 `ApiResponse<T>` 包装
   **And** `GlobalExceptionHandler` 捕获 `RkosException`、`MethodArgumentNotValidException`、通用 `Exception`
   **And** 错误响应包含正确的 HTTP 状态码和业务错误码
   **And** 编写单元测试覆盖主要异常场景

## Tasks / Subtasks

- [x] Task 1：创建 `ApiResponse<T>` 统一响应类（AC: #1）
  - [x] Subtask 1.1：在 `com.rkos.common` 包下创建 `ApiResponse.java`，包含 `code`、`message`、`data`、`timestamp` 四个字段
  - [x] Subtask 1.2：实现 `success(T data)` 和 `error(String code, String message, T data)` 两个静态工厂方法
  - [x] Subtask 1.3：使用 Lombok `@Data`、`@AllArgsConstructor`、`@NoArgsConstructor` 注解
- [x] Task 2：创建 `RkosException` 自定义业务异常（AC: #1）
  - [x] Subtask 2.1：在 `com.rkos.common` 包下创建 `RkosException.java`，继承 `RuntimeException`
  - [x] Subtask 2.2：包含 `errorCode`（String）和 `details`（Object）字段
  - [x] Subtask 2.3：实现三个构造函数：(errorCode, message)、(errorCode, message, Throwable)、(errorCode, message, Object details)
- [x] Task 3：创建 `GlobalExceptionHandler` 全局异常处理器（AC: #1）
  - [x] Subtask 3.1：在 `com.rkos.common` 包下创建 `GlobalExceptionHandler.java`，使用 `@ControllerAdvice` + `@Slf4j`
  - [x] Subtask 3.2：实现 `handleValidationException` — 捕获 `MethodArgumentNotValidException`，返回 400 + 字段级错误明细
  - [x] Subtask 3.3：实现 `handleRkosException` — 捕获 `RkosException`，根据 errorCode 映射 HTTP 状态码
  - [x] Subtask 3.4：实现 `handleGenericException` — 捕获通用 `Exception`，返回 500 + 友好错误消息（禁止暴露堆栈）
  - [x] Subtask 3.5：实现 `getHttpStatus(String errorCode)` 私有方法，映射 errorCode → HttpStatus（含 CONFLICT→409）
- [x] Task 4：创建验证用 Controller（用于测试响应格式）（AC: #1）
  - [x] Subtask 4.1：在 `com.rkos.modules.story.controller` 包下创建临时 `StoryController.java`（桩实现），包含一个 `POST /api/v1/stories` 端点，使用 `@Valid` + `ApiResponse` 包装返回
  - [x] Subtask 4.2：在 `com.rkos.modules.story.dto` 包下创建临时 `StoryRequest.java` DTO，包含 `@NotBlank` 校验注解
- [x] Task 5：编写单元测试（AC: #1）
  - [x] Subtask 5.1：创建 `ApiResponseTest.java` — 4 个测试用例，验证 `success()` 和 `error()` 工厂方法
  - [x] Subtask 5.2：创建 `GlobalExceptionHandlerTest.java` — 7 个测试用例，MockMvc 独立模式验证
  - [x] Subtask 5.3：验证 `MethodArgumentNotValidException` 返回 400 + 字段错误明细
  - [x] Subtask 5.4：验证 `RkosException` 返回对应 HTTP 状态码（NOT_FOUND→404, UNAUTHORIZED→401）
  - [x] Subtask 5.5：验证通用 `Exception` 返回 500 + 友好消息（不含堆栈）
- [x] Task 6：编译与运行验证（AC: #1）
  - [x] Subtask 6.1：`mvn clean compile` 编译通过（BUILD SUCCESS，1.780s）
  - [x] Subtask 6.2：`mvn test` 全部 11 个测试通过（BUILD SUCCESS，3.151s）
  - [x] Subtask 6.3：通过 MockMvc 验证正常响应和各类异常响应均符合 `ApiResponse` 格式

### Review Findings

- [x] [Review][Patch] 添加 `HttpMessageNotReadableException` handler 返回 400 — 非法 JSON 请求应返回 400 Bad Request 而非 500，扩展架构文档定义的三类 handler [GlobalExceptionHandler.java]
- [x] [Review][Patch] `getHttpStatus` null errorCode 导致 NPE — `switch(null)` 抛出 NullPointerException，需添加 null 防护分支 [GlobalExceptionHandler.java:76]
- [x] [Review][Patch] 缺少 `CONFLICT→409` 映射的测试用例 — 代码已实现但测试未覆盖 [GlobalExceptionHandlerTest.java]
- [x] [Review][Patch] 测试注释错别字"兆底"应为"兜底" [GlobalExceptionHandlerTest.java:98]
- [x] [Review][Defer] `ConstraintViolationException` 未专门处理 — deferred, pre-existing（架构文档未定义，超出当前 Story 范围）
- [x] [Review][Defer] `MissingServletRequestPartException` 未专门处理 — deferred, pre-existing（架构文档未定义，超出当前 Story 范围）

## Dev Notes

### 架构规范（强制遵守）

**文件位置**：所有三个核心类放在 `com.rkos.common` 包下：
```
backend/src/main/java/com/rkos/common/
├── ApiResponse.java
├── RkosException.java
└── GlobalExceptionHandler.java
```

[Source: _bmad-output/architecture.md#项目结构模式 L746-749]

**ApiResponse 设计**（架构文档明确规定）：
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "操作成功", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(code, message, data, LocalDateTime.now());
    }
}
```

[Source: _bmad-output/architecture.md#统一响应格式 L494-511]

**RkosException 设计**（架构文档明确规定）：
```java
@Data
@EqualsAndHashCode(callSuper = true)
public class RkosException extends RuntimeException {
    private String errorCode;
    private Object details;

    public RkosException(String errorCode, String message) { ... }
    public RkosException(String errorCode, String message, Throwable cause) { ... }
    public RkosException(String errorCode, String message, Object details) { ... }
}
```

[Source: _bmad-output/architecture.md#自定义异常 L973-996]

**GlobalExceptionHandler 设计**（架构文档明确规定）：

必须处理三类异常，按优先级排列：
1. `MethodArgumentNotValidException` → 400 Bad Request + 字段级错误明细 `Map<String, String>`
2. `RkosException` → 根据 errorCode 映射 HTTP 状态码（UNAUTHORIZED→401, NOT_FOUND→404, 其他→500）
3. `Exception`（兜底） → 500 Internal Server Error + 友好消息，**禁止暴露堆栈**

[Source: _bmad-output/architecture.md#全局异常处理器 L999-1052]

### 强制一致性规则

- **响应格式一致性**：所有 API 必须返回 `ApiResponse<T>` 包装对象
- **异常处理一致性**：所有异常必须通过 `GlobalExceptionHandler` 转换为 `ApiResponse`
- **禁止返回原始堆栈信息**：生产环境只返回友好错误消息

[Source: _bmad-output/architecture.md#强制一致性规则 L1085-1101]

### 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | Spring Boot 4.1 基线 |
| Spring Boot | 4.1.0 | Jakarta EE 11，Spring Framework 7.0 |
| Lombok | Spring Boot 管理 | `@Data`、`@AllArgsConstructor`、`@NoArgsConstructor`、`@Slf4j` |
| JUnit 5 | Spring Boot 管理 | `spring-boot-starter-test` 已包含 |
| MockMvc | Spring Boot 管理 | Controller 层测试 |

### Jackson 3 注意事项

Spring Boot 4.1 + Spring AI 2.0 使用 Jackson 3.x（包名 `tools.jackson`），但 Spring MVC 的 HTTP 消息转换器仍使用 Spring Boot 管理的 Jackson 版本。`ApiResponse` 中 `LocalDateTime` 的序列化由 Spring Boot 自动配置，无需额外处理。

### 前一个 Story 情报（Story 1.2）

| 项目 | 内容 |
|------|------|
| 完成内容 | 双数据库配置启用、Flyway 迁移、HikariCP 连接池 |
| 当前启动类 | `@SpringBootApplication`（无 exclude）+ `@MapperScan` |
| 验证方式 | `mvn spring-boot:run -Dspring-boot.run.profiles=dev` |
| 数据库启动 | 手动 Docker 命令（Story 1.9 才有 docker-compose） |
| Deferred 项 | DEFER-1→Story 2.3（updated_at 触发器）、DEFER-2→Story 1.7（is_active 索引） |

### 关键注意事项

1. **不要重复造轮子**：`ApiResponse`、`RkosException`、`GlobalExceptionHandler` 严格按架构文档实现，不要自行设计字段或方法名
2. **临时 Controller 与 DTO**：Task 4 创建的 `StoryController` 和 `StoryRequest` 是桩实现，用于验证响应格式。后续 Story 1.5 会扩展完善
3. **`Collectors.toMap` 合并冲突**：`handleValidationException` 中使用 `Collectors.toMap` 时，如果同一字段有多条错误会抛 `IllegalStateException`。建议添加 `(existing, replacement) -> existing` 合并函数
4. **`@ControllerAdvice` 扫描**：类在 `com.rkos.common` 包下，被 `@SpringBootApplication` 自动扫描（根包 `com.rkos`），无需额外配置
5. **Lombok `@EqualsAndHashCode(callSuper = true)`**：`RkosException` 继承 `RuntimeException`，必须加 `callSuper = true` 避免警告

### 测试策略

- `ApiResponseTest`：纯 POJO 测试，不需要 Spring 上下文
- `GlobalExceptionHandlerTest`：使用 `@WebMvcTest` + MockMvc，验证 HTTP 响应格式
- 测试需覆盖：正常响应包装、参数校验异常（含多字段）、业务异常（含不同 errorCode→HTTP 状态码映射）、通用异常（500 兜底）
- 测试中使用 `@TestConfiguration` 创建一个临时的 `@RestController` 来触发各种异常

### Project Structure Notes

本 Story 涉及的文件：

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/common/ApiResponse.java` | 新建 | 统一响应包装类 |
| `backend/src/main/java/com/rkos/common/RkosException.java` | 新建 | 自定义业务异常 |
| `backend/src/main/java/com/rkos/common/GlobalExceptionHandler.java` | 新建 | 全局异常处理器 |
| `backend/src/main/java/com/rkos/modules/story/controller/StoryController.java` | 新建 | 桩实现 Controller（验证用） |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryRequest.java` | 新建 | 桩实现 DTO（验证用） |
| `backend/src/test/java/com/rkos/common/ApiResponseTest.java` | 新建 | ApiResponse 单元测试 |
| `backend/src/test/java/com/rkos/common/GlobalExceptionHandlerTest.java` | 新建 | 异常处理器集成测试 |

### References

- [Source: _bmad-output/epics.md#Story 1.3：统一响应格式与全局异常处理] — 验收标准定义
- [Source: _bmad-output/architecture.md#决策6：API 安全基线] — ApiResponse、参数校验、全局异常处理的完整代码示例
- [Source: _bmad-output/architecture.md#项目结构模式 L746-749] — common/ 包下文件位置
- [Source: _bmad-output/architecture.md#强制一致性规则 L1085-1101] — 响应格式和异常处理的强制规则
- [Source: _bmad-output/1-2-dual-database-config-migration.md] — 前一 Story 完成情况和 Deferred 项
- [Source: _bmad-output/1-1-project-skeleton-init.md#技术栈版本锁定] — Java 21 + Spring Boot 4.1.0 版本信息

## Dev Agent Record

### Agent Model Used

Qoder AI（当前会话）

### Debug Log References

- Java 21 环境检测：系统 jenv 默认指向 Java 17，需通过 `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` 指定
- `mvn clean compile` 编译成功（12 个源文件，1.780s）
- `mvn test` 全部 11 个测试通过（3.151s）
- `Collectors.toMap` 合并冲突已处理：添加 `(existing, replacement) -> existing` 合并函数
- `getHttpStatus` 使用 Java 21 switch 表达式，含 4 个状态码映射（UNAUTHORIZED/NOT_FOUND/VALIDATION_ERROR/CONFLICT）
- `invalidJson` 测试修正：非法 JSON 触发 `HttpMessageNotReadableException`，被兆底 `Exception` handler 捕获返回 500，符合当前架构规范
- MockMvc 使用独立模式（`standaloneSetup`），不加载 Spring 上下文，避免数据库/Spring AI 自动配置干扰

### Completion Notes List

- ✅ `ApiResponse<T>` — 统一响应包装类，含 `success()` 和 `error()` 静态工厂方法
- ✅ `RkosException` — 自定义业务异常，含 `errorCode` 和 `details` 字段，三个构造函数
- ✅ `GlobalExceptionHandler` — 三类异常处理：参数校验(400)、业务异常(动态状态码)、兆底(500)
- ✅ `StoryController` + `StoryRequest` — 桩实现，用于验证响应格式
- ✅ 单元测试 11 个用例全部通过：ApiResponseTest(4) + GlobalExceptionHandlerTest(7)
- ⚠️ `getHttpStatus` 额外增加了 `CONFLICT→409` 映射（架构文档未明确，但 Story 2.7 重新处理需要 409 状态码）

### File List

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/common/ApiResponse.java` | 新建 | 统一响应包装类 |
| `backend/src/main/java/com/rkos/common/RkosException.java` | 新建 | 自定义业务异常 |
| `backend/src/main/java/com/rkos/common/GlobalExceptionHandler.java` | 新建 | 全局异常处理器 |
| `backend/src/main/java/com/rkos/modules/story/controller/StoryController.java` | 新建 | 桩实现 Controller |
| `backend/src/main/java/com/rkos/modules/story/dto/StoryRequest.java` | 新建 | 桩实现 DTO |
| `backend/src/test/java/com/rkos/common/ApiResponseTest.java` | 新建 | ApiResponse 单元测试（4 用例） |
| `backend/src/test/java/com/rkos/common/GlobalExceptionHandlerTest.java` | 新建 | 异常处理器测试（7 用例） |

### Change Log

- 2025-07-16：Story 1.3 实施完成 — ApiResponse 统一响应、RkosException 业务异常、GlobalExceptionHandler 全局异常处理、桩 Controller/DTO、11 个单元测试全部通过
