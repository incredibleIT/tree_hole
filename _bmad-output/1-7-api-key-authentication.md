# Story 1.7：API Key 认证

Status: done

## Story

作为**系统维护者**，
我希望所有 API 端点有基础的 API Key 认证保护，
以便防止未授权访问。

## Acceptance Criteria

1. **Given** `api_keys` 表中存在有效的 API Key 记录（`key_hash` 为 SHA-256 哈希存储，`is_active = true`，未过期）
   **When** 请求 Header 包含有效的 `X-API-Key`
   **Then** 请求正常通过，到达对应 Controller

2. **Given** 请求 Header 缺少 `X-API-Key` 或 Key 无效（不存在、已禁用、已过期）
   **When** 访问任何受保护的 `/api/v1/**` 端点
   **Then** 返回 HTTP 401 Unauthorized，响应体为 `{"code":"UNAUTHORIZED","message":"无效的 API Key","data":null}`

3. **Given** API Key 以 SHA-256 哈希方式存储在 `api_keys` 表（NFR6）
   **When** 系统验证 API Key
   **Then** 对传入的明文 Key 计算 SHA-256 哈希后与数据库 `key_hash` 比对，不存明文

4. **Given** `/api/v1/health` 端点不受认证保护（公开访问）
   **When** 发送 `GET /api/v1/health`（不携带 `X-API-Key`）
   **Then** 请求正常通过，不返回 401

5. **Given** Swagger UI 和 API 文档端点不受认证保护
   **When** 访问 `/swagger-ui/**`、`/v3/api-docs/**`
   **Then** 请求正常通过

## Tasks / Subtasks

- [x] Task 1：创建 `ApiKey` 实体模型（AC: #1, #3）
  - [x] Subtask 1.1：在 `com.rkos.modules.auth.model` 包下创建 `ApiKey.java`
  - [x] Subtask 1.2：使用 MyBatis-Plus `@TableName("api_keys")` 注解映射 PostgreSQL `api_keys` 表
  - [x] Subtask 1.3：字段包含 id、keyHash、name、isActive、createdAt、expiresAt
- [x] Task 2：创建 `ApiKeyMapper` MyBatis-Plus Mapper（AC: #1, #3）
  - [x] Subtask 2.1：在 `com.rkos.modules.auth.mapper` 包下创建 `ApiKeyMapper.java`
  - [x] Subtask 2.2：继承 `BaseMapper<ApiKey>`，添加 `selectByKeyHash` 方法
- [x] Task 3：创建 `ApiKeyAuthService` 认证服务（AC: #1, #2, #3）
  - [x] Subtask 3.1：在 `com.rkos.modules.auth.service` 包下创建 `ApiKeyAuthService.java`
  - [x] Subtask 3.2：实现 `validate(String rawKey)` 方法 — SHA-256 哈希 → 数据库查询 → 校验 isActive + expiresAt
  - [x] Subtask 3.3：提供静态工具方法 `hashKey(String rawKey)` 计算 SHA-256（供测试和迁移使用）
- [x] Task 4：创建 `ApiKeyAuthInterceptor` 拦截器（AC: #1, #2, #4）
  - [x] Subtask 4.1：在 `com.rkos.modules.auth.service` 包下创建 `ApiKeyAuthInterceptor.java`
  - [x] Subtask 4.2：实现 `HandlerInterceptor.preHandle` — 提取 `X-API-Key` Header → 调用 `ApiKeyAuthService.validate`
  - [x] Subtask 4.3：验证失败时写入 401 响应（`Content-Type: application/json` + `ApiResponse` 格式 JSON）
- [x] Task 5：创建 `ApiKeyAuthConfig` 配置类（AC: #4, #5）
  - [x] Subtask 5.1：在 `com.rkos.config` 包下创建 `ApiKeyAuthConfig.java`
  - [x] Subtask 5.2：实现 `WebMvcConfigurer.addInterceptors` — 拦截 `/api/v1/**`
  - [x] Subtask 5.3：排除路径：`/api/v1/health`、`/swagger-ui/**`、`/v3/api-docs/**`
- [x] Task 6：创建 Flyway V2 迁移脚本 — 种子开发用 API Key（AC: #1, #3）
  - [x] Subtask 6.1：创建 `V2__seed_dev_api_key.sql`，向 `api_keys` 表插入一条开发环境 Key
  - [x] Subtask 6.2：`key_hash` = SHA-256("dev-api-key-12345")，`name` = 'Development Key'，`is_active` = true
- [x] Task 7：配置 `application-dev.yml` 和 `application.yml`（AC: #1）
  - [x] Subtask 7.1：确认 `application-dev.yml` 已有 `rkos.api.key: dev-api-key-12345`
  - [x] Subtask 7.2：在 `application.yml` 添加 `rkos.api.enabled` 开关配置项（默认 true）
- [x] Task 8：编写 `ApiKeyAuthInterceptorTest` 测试（AC: #1, #2, #4）
  - [x] Subtask 8.1：测试有效 Key → 请求通过
  - [x] Subtask 8.2：测试缺少 Header → 401
  - [x] Subtask 8.3：测试无效 Key → 401
  - [x] Subtask 8.4：测试过期 Key → 401
  - [x] Subtask 8.5：测试禁用 Key → 401

## Dev Notes

### 架构规范（强制遵守）

**文件位置**：
```
backend/src/main/java/com/rkos/
├── config/
│   ├── package-info.java              # 已存在
│   └── ApiKeyAuthConfig.java          # 新建
├── modules/
│   └── auth/                          # 新建模块
│       ├── model/
│       │   └── ApiKey.java            # 新建（MyBatis-Plus 实体）
│       ├── mapper/
│       │   └── ApiKeyMapper.java      # 新建（MyBatis-Plus Mapper）
│       └── service/
│           ├── ApiKeyAuthService.java # 新建（认证服务）
│           └── ApiKeyAuthInterceptor.java # 新建（拦截器）
```

**重要**：`@MapperScan("com.rkos.modules.*.mapper")` 在 `RkosApplication.java` 中配置，使用 `modules/auth/mapper` 目录可被自动扫描覆盖，无需修改 MapperScan 配置。

[Source: _bmad-output/architecture.md#项目结构模式 L740-796]
[Source: backend/src/main/java/com/rkos/RkosApplication.java L14]

**认证拦截器模式**（架构文档定义的参考实现）：
```java
// 架构文档 L380-413 给出的实现参考
@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String apiKey = request.getHeader("X-API-Key");
        if (!expectedApiKey.equals(apiKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"code\":\"UNAUTHORIZED\",\"message\":\"无效的 API Key\"}"
            );
            return false;
        }
        return true;
    }
}
```

**重要改进**：架构参考实现使用简单字符串比较（`@Value` 注入配置值），本 Story 改进为数据库验证方式：
- 使用 `api_keys` 表（V1 迁移已创建）存储 SHA-256 哈希
- 支持多 Key、Key 禁用（`is_active`）、Key 过期（`expires_at`）
- 符合 NFR6（API Key 哈希存储）

**401 响应格式**（拦截器层直接写入，不经过 GlobalExceptionHandler）：
```json
{
  "code": "UNAUTHORIZED",
  "message": "无效的 API Key",
  "data": null,
  "timestamp": "2026-07-19T10:30:00"
}
```

**注意**：拦截器在 Controller 之前执行，异常不经过 `@ControllerAdvice`，因此 401 响应由拦截器直接构造 JSON 写入 `HttpServletResponse`。`GlobalExceptionHandler` 中已有 `"UNAUTHORIZED" → 401` 映射（供其他地方抛 `RkosException("UNAUTHORIZED")` 时使用），两者不冲突。

[Source: _bmad-output/architecture.md#API Key 认证 L376-413]

### api_keys 表结构（V1 迁移已创建）

```sql
CREATE TABLE api_keys (
    id              BIGSERIAL       PRIMARY KEY,
    key_hash        VARCHAR(64)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP
);
CREATE UNIQUE INDEX idx_api_keys_key_hash ON api_keys(key_hash);
```

[Source: _bmad-output/architecture.md#api_keys 表 L1543-1553]
[Source: backend/src/main/resources/db/migration/V1__init_schema.sql L54-64]

### 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | Spring Boot 4.1 基线 |
| Spring Boot | 4.1.0 | Jakarta EE 11，Spring Framework 7.0 |
| MyBatis-Plus | 3.5.16 | `mybatis-plus-spring-boot4-starter`，PostgreSQL Mapper |
| PostgreSQL | Docker 容器 | `api_keys` 表由 Flyway V1 创建 |
| JUnit 5 | Spring Boot 管理 | `spring-boot-starter-test` |

### SHA-256 哈希实现指导

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public static String hashKey(String rawKey) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA-256 算法不可用", e);
    }
}
```

`dev-api-key-12345` 的 SHA-256 哈希值需在实现时计算并写入 V2 迁移脚本。

### MyBatis-Plus Mapper 配置

- `application-dev.yml` 已配置 `mybatis-plus.mapper-locations: classpath:mapper/**/*.xml`
- `@MapperScan("com.rkos.modules.*.mapper")` 在 `RkosApplication.java` 中配置，`modules/auth/mapper` 自动被覆盖

### 前一个 Story 情报（Story 1.6）

| 项目 | 内容 |
|------|------|
| 完成内容 | StoryDetailResponse + StoryPageResponse DTO + Repository/Service/Controller 扩展 + 测试 |
| 已有测试总数 | 38 个（ApiResponseTest 4 + GlobalExceptionHandlerTest 8 + StoryMongoRepositoryTest 8 + StoryControllerTest 9 + StoryServiceTest 9） |
| MockMvc 模式 | `standaloneSetup` + `@ExtendWith(MockitoExtension.class)` |
| Java 环境 | `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` |
| 构建工具 | Maven（`mvn test`） |
| Deferred 项 | 空字符串参数过滤、分页参数校验（不涉及本 Story） |

**关键注意事项**：
1. **不修改 StoryController / StoryService / StoryMongoRepository**：本 Story 仅添加认证拦截器
2. **不修改 GlobalExceptionHandler**：`UNAUTHORIZED` 映射已就绪
3. **不修改 ApiResponse**：拦截器中直接构造 JSON 字符串写入 response
4. **PostgreSQL 连接**：测试需要连接 PostgreSQL（Flyway 迁移创建表），或使用 H2 内存数据库
5. **测试切片**：拦截器测试使用 `@WebMvcTest` 或 MockMvc standalone + Mock `ApiKeyAuthService`
6. **MapperScan 路径**：需验证 PostgresConfig 的 `@MapperScan` 覆盖新的 `com.rkos.common.auth` 包

### 本 Story 不涉及的范围

- Story 1-8 Swagger 配置（仅排除 Swagger 路径）
- Story 1-9 Docker Compose（仅修改应用配置）
- Story 2-1 LLM 调用服务
- API Key 管理端点（CRUD，未来 Story）
- OAuth 2.0 认证（未来迭代）

### 测试策略

- **ApiKeyAuthInterceptorTest**（新建，核心测试）：
  - 使用 MockMvc standalone 模式 + Mock `ApiKeyAuthService`
  - 创建一个简单的测试 Controller（如 `@GetMapping("/api/v1/test")` 返回 200）
  - 测试用例：
    1. 有效 Key → 200（Service.validate 返回 true）
    2. 缺少 X-API-Key Header → 401
    3. 无效 Key → 401（Service.validate 返回 false）
    4. Key 已过期 → 401
    5. Key 已禁用 → 401
    6. 排除路径（如 /api/v1/health）→ 无 Key 也通过
- **ApiKeyAuthServiceTest**（新建，可选）：
  - Mock `ApiKeyMapper`，测试 hashKey + validate 逻辑
  - 验证 SHA-256 哈希计算正确性
  - 验证 isActive + expiresAt 校验逻辑

### 拦截器排除路径清单

| 路径模式 | 说明 |
|----------|------|
| `/api/v1/health` | 健康检查端点（Story 4-1 实现，但路径需预留排除） |
| `/swagger-ui/**` | Swagger UI 静态资源 |
| `/v3/api-docs/**` | OpenAPI 文档 JSON |
| `/actuator/**` | Actuator 管理端点（不走 `/api/v1/` 前缀，无需排除） |

### MyBatis-Plus 实体映射注意

- 表名 `api_keys` → 类名 `ApiKey`，使用 `@TableName("api_keys")`
- 字段映射（snake_case → camelCase）：
  - `key_hash` → `keyHash`
  - `is_active` → `isActive`
  - `created_at` → `createdAt`
  - `expires_at` → `expiresAt`
- MyBatis-Plus 已配置 `map-underscore-to-camel-case: true`，自动转换无需 `@TableField`

### Project Structure Notes

本 Story 新增/修改的文件：

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/modules/auth/model/ApiKey.java` | 新建 | MyBatis-Plus 实体 |
| `backend/src/main/java/com/rkos/modules/auth/mapper/ApiKeyMapper.java` | 新建 | MyBatis-Plus Mapper |
| `backend/src/main/java/com/rkos/modules/auth/service/ApiKeyAuthService.java` | 新建 | 认证服务 |
| `backend/src/main/java/com/rkos/modules/auth/service/ApiKeyAuthInterceptor.java` | 新建 | 拦截器 |
| `backend/src/main/java/com/rkos/config/ApiKeyAuthConfig.java` | 新建 | WebMvc 配置 |
| `backend/src/main/resources/db/migration/V2__seed_dev_api_key.sql` | 新建 | 种子开发 Key |
| `backend/src/test/java/com/rkos/modules/auth/ApiKeyAuthInterceptorTest.java` | 新建 | 拦截器测试 |
| `backend/src/main/resources/application.yml` | 修改 | 添加 rkos.api.enabled 配置 |

不修改的文件：

| 文件 | 说明 |
|------|------|
| `StoryController.java` | 不修改，拦截器对所有 Controller 透明 |
| `StoryService.java` | 不修改 |
| `GlobalExceptionHandler.java` | UNAUTHORIZED 映射已就绪 |
| `ApiResponse.java` | 不修改 |
| `RkosException.java` | 不修改 |
| `StoryControllerTest.java` | 不修改（standalone 模式不加载拦截器，已有测试不受影响） |

### References

- [Source: _bmad-output/epics.md#Story 1.7：API Key 认证 L227-242] — 验收标准定义
- [Source: _bmad-output/architecture.md#API Key 认证 L376-421] — 拦截器参考实现 + 环境变量配置
- [Source: _bmad-output/architecture.md#项目结构模式 L740-796] — config + common 目录结构
- [Source: _bmad-output/architecture.md#api_keys 表 L1543-1553] — 表结构定义
- [Source: _bmad-output/architecture.md#强制一致性规则 L1085-1101] — 命名/响应/异常一致性
- [Source: backend/src/main/resources/db/migration/V1__init_schema.sql L54-64] — api_keys 表 DDL
- [Source: backend/src/main/resources/application-dev.yml L54-56] — `rkos.api.key: dev-api-key-12345`
- [Source: _bmad-output/1-6-story-query-api.md] — 前一个 Story 完成情况
- [Source: _bmad-output/deferred-work.md] — 延迟工作项

## Dev Agent Record

### Agent Model Used

Qoder AI

### Debug Log References

- `@DataMongoTest` 上下文加载 `RkosApplication` 时，`@MapperScan` 扫描到新 `ApiKeyMapper`，缺少 `sqlSessionFactory`（DataSource）导致 `StoryMongoRepositoryTest` 失败。通过创建 `MongoTestMapperFixConfig`（`BeanDefinitionRegistryPostProcessor + PriorityOrdered`）在 Bean 实例化前移除 `MapperFactoryBean` 定义解决。

### Completion Notes List

- 全部 8 个 Task 完成，所有 AC 满足
- 新增 15 个测试（ApiKeyAuthInterceptorTest 6 + ApiKeyAuthServiceTest 9），全部通过
- 全量测试 53 个全部通过（38 原有 + 15 新增），无回归
- 拦截器测试采用直接调用 `preHandle` + `MockHttpServletRequest/Response` 方式，避免 MockMvc standalone 模式的路径过滤限制
- SHA-256 哈希值 `8264dc9f07e749d9c2ffead0b25de8cb22bed7af774e189ef224ae015908776b` 已写入 V2 迁移脚本并经过测试验证
- 修复了 `@DataMongoTest` 与 `@MapperScan` 的兼容性问题：新增 `MongoTestMapperFixConfig` 移除测试切片上下文中不需要的 MyBatis Mapper Bean 定义

### File List

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/modules/auth/model/ApiKey.java` | 新建 | MyBatis-Plus 实体，`@TableName("api_keys")` |
| `backend/src/main/java/com/rkos/modules/auth/mapper/ApiKeyMapper.java` | 新建 | MyBatis-Plus Mapper，`selectByKeyHash` 默认方法 |
| `backend/src/main/java/com/rkos/modules/auth/service/ApiKeyAuthService.java` | 新建 | 认证服务，`validate` + `hashKey` |
| `backend/src/main/java/com/rkos/modules/auth/service/ApiKeyAuthInterceptor.java` | 新建 | `HandlerInterceptor`，校验 `X-API-Key` |
| `backend/src/main/java/com/rkos/config/ApiKeyAuthConfig.java` | 新建 | WebMvc 配置，拦截 `/api/v1/**` + 排除路径 |
| `backend/src/main/resources/db/migration/V2__seed_dev_api_key.sql` | 新建 | 种子开发 Key（SHA-256 哈希） |
| `backend/src/main/resources/application.yml` | 修改 | 添加 `rkos.api.enabled: true` |
| `backend/src/test/java/com/rkos/modules/auth/ApiKeyAuthInterceptorTest.java` | 新建 | 拦截器单元测试（6 个测试） |
| `backend/src/test/java/com/rkos/modules/auth/ApiKeyAuthServiceTest.java` | 新建 | 认证服务单元测试（9 个测试） |
| `backend/src/test/java/com/rkos/modules/story/repository/MongoTestMapperFixConfig.java` | 新建 | `@DataMongoTest` Mapper Bean 兼容修复 |
| `backend/src/test/java/com/rkos/modules/story/repository/StoryMongoRepositoryTest.java` | 修改 | 添加 `@Import(MongoTestMapperFixConfig.class)` |

### Change Log

| 日期 | 变更摘要 |
|------|----------|
| 2025-07-16 | Story 1-7 实现完成：API Key 认证拦截器 + SHA-256 哈希存储 + 数据库验证 + Flyway V2 种子 + 测试 15 个 |
