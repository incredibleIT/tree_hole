---
stepsCompleted:
  - step-01-init
  - step-02-context
  - step-03-starter
  - step-04-decisions
  - step-05-patterns
  - step-06-structure
  - step-07-validation
  - step-08-complete
lastStep: 8
status: 'complete'
completedAt: '2026-07-15'
inputDocuments:
  - _bmad-output/prd.md
  - _bmad-output/product-brief-rkos.md
  - _bmad-output/product-brief-rkos-distillate.md
workflowType: 'architecture'
project_name: '以外 RKOS'
user_name: 'Yang'
date: '2026-07-15'
---

# 架构决策文档 - 以外 RKOS（关系知识操作系统）

_本文档通过逐步协作发现构建。章节随我们一起推进各个架构决策而逐步追加。_

## 项目上下文分析

### 需求概览

**功能需求：**
31 个功能需求，组织为 8 个能力域。架构核心链路为：**故事提交 → 内容安全过滤 → Story Understanding Agent 抽取 → 双存储写入（MongoDB + PostgreSQL）→ 确认反馈**。这条链路贯穿了 5 个能力域，是系统的主干。

**非功能需求：**
21 个非功能需求中，对架构影响最大的是：
- LLM 可插拔设计（NFR12/15）— 要求抽象服务层
- 单机到多实例的平滑迁移（NFR10）— 要求无状态设计
- Prompt 配置外置且运行时可调（NFR20）— 要求配置热更新机制
- LLM 调用的独立限流和重试（NFR17/FR17）— 要求弹性调用模式

**规模与复杂度：**
- 主要领域：API 优先的后端系统（第一迭代无前端）
- 复杂度级别：中高
- 预估架构组件数：约 8-10 个核心组件

### 技术约束与依赖

| 约束 | 说明 |
|------|------|
| 后端框架 | Java（Spring Boot）— 已确定 |
| 双存储 | MongoDB（非结构化）+ PostgreSQL（结构化）— 已确定 |
| LLM 框架 | Spring AI（核心抽象层）+ Spring AI Alibaba（阿里云提供商，按需引入） |
| Agent 模式 | 多轮对话，需 ChatMemory 持久化 |
| Prompt 管理 | 文件外置，运行时可调 |
| 模型策略 | 支持 OpenAI / 通义千问 / 本地 Ollama 等多提供商切换 |
| 部署方式 | Docker Compose 一键启动 |
| 团队规模 | 单人开发维护 |
| 项目性质 | 公益开源 |
| API 风格 | RESTful，`/api/v1/` 前缀，JSON 格式 |
| 认证方式 | API Key（第一迭代）→ OAuth 2.0（未来） |

### 跨切面关注点

1. **双存储数据一致性**：MongoDB 和 PostgreSQL 之间通过故事 ID 关联，需要保证写入一致性和查询关联
2. **LLM 调用弹性**：超时、重试、限流、成本监控、提供商切换
3. **Agent 编排**：Story Understanding Agent 的调用链路、Prompt 管理、置信度传递
4. **API 安全与治理**：认证、限流、版本管理、审计日志
5. **可观测性**：结构化日志、健康检查、调用统计
6. **配置管理**：Prompt 外置、运行时可调、环境隔离

## 启动器模板评估

### Spring Boot / Spring AI 版本矩阵（2026-07）

| 组件 | 最新稳定版 | 发布时间 | 基线依赖 |
|------|-----------|---------|----------|
| **Spring Boot** | 4.1.0 | 2026-06 | Java 21+ |
| Spring Boot 3.x | 3.5.16 | 2026-06 | Java 17+ |
| **Spring AI** | 2.0.0 GA | 2026-06-12 | Spring Boot 4.1+ |
| Spring AI 1.x | 1.1.4 | 2026-05 | Spring Boot 3.5+ |
| **Spring AI Alibaba** | 1.1.2.0 | 2026-05 | Spring AI 1.1.x + Boot 3.x |

**关键发现：**
- Spring AI 2.0 要求 Spring Boot 4.1+，但 Spring AI Alibaba 目前仍基于 Spring AI 1.1.x（对齐 Boot 3.x）
- 两者暂不在同一基线，无法直接同时使用
- **过渡方案**：先用 Spring AI 原生 DashScope starter 接入通义千问，等 Alibaba 发布 2.0 版本后再切换

### 推荐启动方案：前沿路线

**选择：Spring Boot 4.1.0 + Spring AI 2.0.0**

**理由：**
1. **架构前瞻性**：RKOS 是长期演进系统，采用最新稳定版减少未来迁移成本
2. **自纠正结构化输出**：Spring AI 2.0 的核心特性，对 Story Understanding Agent 至关重要
3. **MCP 原生集成**：为后续工具调用预留能力
4. **Java 21 LTS**：性能提升、虚拟线程支持，适合高并发 LLM 调用场景
5. **Alibaba 延迟兼容策略**：当前用 Spring AI 原生 DashScope starter，等 Alibaba 2.0 发布后无缝切换

### 项目初始化命令

```bash
# 使用 Spring Initializr 生成项目骨架
mkdir rkos-backend && cd rkos-backend

spring init \
  --name=rkos-backend \
  --package-name=com.rkos \
  --dependencies=web,validation,lombok,actuator,mongodb,data-jpa,postgresql \
  --boot-version=4.1.0 \
  --java-version=21 \
  --build=maven

# 添加 Spring AI 依赖（手动编辑 pom.xml）
# <dependency>
#   <groupId>org.springframework.ai</groupId>
#   <artifactId>spring-ai-starter-model-openai</artifactId>
#   <version>2.0.0</version>
# </dependency>
# <dependency>
#   <groupId>org.springframework.ai</groupId>
#   <artifactId>spring-ai-starter-model-dashscope</artifactId>
#   <version>2.0.0</version>
# </dependency>
```

### 架构决策记录

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 启动器模板 | Spring Boot 4.1 + Spring AI 2.0 | 前沿路线，减少未来迁移成本 |
| LLM 框架层级 | Spring AI（核心）+ Spring AI Alibaba（按需） | 抽象层统一，提供商插件化 |
| 模型提供商接入 | 先 DashScope（通义千问），后 OpenAI | 阿里云生态优先，成本可控 |
| Java 版本 | Java 21 LTS | 性能、虚拟线程、长期支持 |
| 构建工具 | Maven | Spring 官方首选，依赖管理成熟 |

## 核心架构决策

### 决策优先级分析

**关键决策（影响实现）：**
1. **四存储演进策略**：第一迭代双存储（MongoDB + PostgreSQL），后续扩展为四层（+ Milvus + Neo4j）
2. **数据职责分离**：MongoDB 存原始数据，PostgreSQL 存 Agent 结构化数据，应用层控制一致性
3. **Agent 实现方式**：Spring AI 原生 API（ChatClient、PromptTemplate、ToolCalling）
4. **记忆持久化**：PostgreSQL（`JdbcChatMemoryRepository`）
5. **Prompt 热更新**：Spring Cloud Config（完整方案，含 Config Server 部署）
6. **API 安全基线**：最小实现（API Key 认证 + 参数校验 + 全局异常处理）
7. **双环境配置**：dev/prod Profile，Docker Compose 多环境支持

**推迟决策（MVP 后）：**
- 限流与熔断（暂不实现）
- 审计日志（暂不实现）
- Milvus 向量库集成（第二迭代）
- Neo4j 知识图谱集成（第三迭代）

---

### 数据存储架构

#### 决策 1：四存储演进策略

**选择**：分阶段引入四存储架构

| 迭代 | 存储组件 | 用途 | 状态 |
|------|---------|------|------|
| 第一迭代 | MongoDB | 原始故事、图片、聊天记录 | ✅ 已确定 |
| 第一迭代 | PostgreSQL | Agent 结构化数据（Genome、Rule、Pattern） | ✅ 已确定 |
| 第二迭代 | Milvus | Embedding 向量索引 | ⏳ 待引入 |
| 第三迭代 | Neo4j | 关系知识图谱 | ⏳ 待引入 |

**理由**：
- 降低第一迭代复杂度，聚焦核心链路（故事提交 → Agent 抽取 → 双存储写入）
- PostgreSQL 表设计预留 `embedding` 字段（JSONB 类型），方便后续迁移到 Milvus
- 通过故事 ID 关联 MongoDB 和 PostgreSQL，保证查询一致性

#### 决策 2：数据一致性控制

**选择**：应用层协调（非事件驱动）

**实现策略**：
```java
@Service
public class StoryPersistenceService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Transactional
    public void saveStoryWithGenome(Story story, RelationshipGenome genome) {
        // 1. 先写 MongoDB（原始数据）
        mongoTemplate.save(story);
        
        // 2. 成功后再写 PostgreSQL（结构化数据）
        String sql = "INSERT INTO relationship_genomes (story_id, genome_data, created_at) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, story.getId(), genome.toJson(), LocalDateTime.now());
        
        // 3. 失败时 MongoDB 记录标记为 pending，定时任务补偿
    }
}
```

**补偿机制**：
- MongoDB 文档增加 `sync_status` 字段（pending/synced/failed）
- 定时任务扫描 pending 状态，重试同步到 PostgreSQL
- 失败超过 3 次告警通知

---

### Agent 编排与调用

#### 决策 3：Spring AI 原生 API

**选择**：使用 Spring AI 提供的核心抽象

**核心组件**：
- `ChatClient`：统一的 LLM 调用接口
- `PromptTemplate`：模板化 Prompt 管理
- `ToolCallingManager`：工具调用编排（未来 MCP 集成）
- `ChatMemory`：多轮对话记忆

**示例代码**：
```java
@Service
public class StoryUnderstandingAgent {
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private PromptTemplateService promptTemplateService;
    
    public RelationshipGenome understandStory(String storyContent) {
        // 加载系统提示词和用户模板
        String systemPrompt = promptTemplateService.loadSystemPrompt("story-understanding");
        String userTemplate = promptTemplateService.loadUserTemplate("story-understanding");
        
        // 构建 Prompt
        Prompt prompt = new Prompt(
            List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userTemplate.replace("{{story}}", storyContent))
            )
        );
        
        // 调用 LLM
        ChatResponse response = chatClient.call(prompt);
        
        // 解析结构化输出
        return parseGenome(response.getResult().getOutput().getContent());
    }
}
```

#### 决策 4：记忆持久化（PostgreSQL）

**选择**：`JdbcChatMemoryRepository`

**配置**：
```yaml
spring:
  ai:
    chat:
      memory:
        repository:
          type: jdbc
          jdbc:
            table-name: chat_memories
            schema: public
```

**数据库表结构**：
```sql
CREATE TABLE chat_memories (
    id SERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    message_content TEXT NOT NULL,
    message_type VARCHAR(50) NOT NULL,  -- USER / ASSISTANT / SYSTEM
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id)
);
```

**会话隔离策略**：
- 每个用户一个 `conversation_id`（UUID）
- 记忆保留最近 10 轮对话（可配置）
- 定期清理过期记忆（TTL 7 天）

#### 决策 5：Prompt 热更新（Spring Cloud Config）

**选择**：完整 Spring Cloud Config 方案

**目录结构**：
```
src/main/resources/prompts/
├── story-understanding/
│   ├── system-prompt.txt         # 系统提示词（固定）
│   └── user-template.txt         # 用户输入模板（含变量占位符）
── relationship-rule/
│   ├── system-prompt.txt
│   └── user-template.txt
── relationship-pattern/
│   ├── system-prompt.txt
│   └── user-template.txt
├── feedback-understanding/
│   ├── system-prompt.txt
│   └── user-template.txt
└── relationship-ai/
    ├── system-prompt.txt
    └── user-template.txt
```

**Config Server Docker Compose**：
```yaml
# docker-compose.config.yml
services:
  config-server:
    image: springcloud/spring-cloud-config-server:4.1.0
    ports:
      - "8888:8888"
    environment:
      SPRING_CLOUD_CONFIG_SERVER_GIT_URI: https://github.com/your-org/rkos-config.git
      SPRING_PROFILES_ACTIVE: native
    volumes:
      - ./config-repo:/tmp/config-repo
```

**客户端配置**：
```yaml
spring:
  cloud:
    config:
      uri: http://localhost:8888
      name: rkos-backend
      profile: ${SPRING_PROFILES_ACTIVE:dev}
      fail-fast: true

management:
  endpoints:
    web:
      exposure:
        include: refresh,health,info

rkos:
  prompts:
    base-path: classpath:/prompts/
```

**热更新触发**：
```bash
# POST /actuator/refresh 触发配置刷新
curl -X POST http://localhost:8080/actuator/refresh
```

**Git 仓库结构（Config Repo）**：
```
rkos-config/
├── rkos-backend-dev.yml      # 开发环境配置
├── rkos-backend-prod.yml     # 生产环境配置
└── application.yml            # 公共配置
```

---

### API 设计与安全

#### 决策 6：最小安全基线

**选择**：仅实现三项基础安全措施

##### 1. API Key 认证

**实现**：
```java
@Configuration
public class ApiKeyAuthConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiKeyAuthInterceptor())
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/health", "/api/v1/docs");
    }
}

@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {
    
    @Value("${rkos.api.key}")
    private String expectedApiKey;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
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

**环境变量注入**：
```yaml
# application.yml
rkos:
  api:
    key: ${RKOS_API_KEY:default-dev-key}  # 生产环境必须设置环境变量
```

##### 2. 参数校验（Bean Validation）

**实现**：
```java
@RestController
@RequestMapping("/api/v1/stories")
public class StoryController {
    
    @PostMapping
    public ResponseEntity<ApiResponse<StoryResponse>> submitStory(
            @Valid @RequestBody StoryRequest request) {
        // Spring Boot 自动校验 @NotNull, @NotBlank 等注解
        StoryResponse response = storyService.submit(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

@Data
public class StoryRequest {
    @NotBlank(message = "故事内容不能为空")
    @Size(max = 10000, message = "故事内容不能超过 10000 字")
    private String content;
    
    @Size(max = 10, message = "最多上传 10 张图片")
    private List<String> images;
    
    @Pattern(regexp = "^(text|image|chat)$", message = "无效的故事类型")
    private String type;
}
```

##### 3. 全局异常处理

**实现**：
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage
                ));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", "参数校验失败", errors));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("未捕获的异常", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "系统内部错误", null));
    }
}
```

**统一响应格式**：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": { ... },
  "timestamp": "2026-07-15T10:30:00Z"
}
```

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

---

### 基础设施与部署

#### 决策 7：双环境配置策略

**选择**：Spring Boot Profile + Docker Compose 多环境支持

**配置文件结构**：
```
src/main/resources/
├── application.yml               # 公共配置
├── application-dev.yml           # 开发环境覆盖
└── application-prod.yml          # 生产环境覆盖
```

**关键差异对比**：

| 配置项 | dev | prod |
|--------|-----|------|
| LLM 提供商 | DashScope（测试 Key） | DashScope（生产 Key，环境变量注入） |
| 数据库连接 | localhost:27017 / localhost:5432 | mongodb-service:27017 / postgresql-service:5432 |
| 日志级别 | DEBUG | INFO/WARN |
| Actuator | 全部暴露（/actuator/**） | 仅 health/info（/actuator/health, /actuator/info） |
| Config Server | http://localhost:8888 | http://config-server:8888 |

**application-dev.yml**：
```yaml
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: rkos_dev
  datasource:
    url: jdbc:postgresql://localhost:5432/rkos_dev
    username: dev_user
    password: dev_password

logging:
  level:
    root: DEBUG
    com.rkos: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: "*"  # 开发环境暴露所有端点

rkos:
  api:
    key: dev-api-key-12345
  llm:
    provider: dashscope
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:test-key}
      model: qwen-max
```

**application-prod.yml**：
```yaml
spring:
  data:
    mongodb:
      host: mongodb-service
      port: 27017
      database: rkos_prod
  datasource:
    url: jdbc:postgresql://postgresql-service:5432/rkos_prod
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

logging:
  level:
    root: WARN
    com.rkos: INFO

management:
  endpoints:
    web:
      exposure:
        include: health,info  # 生产环境仅暴露必要端点

rkos:
  api:
    key: ${RKOS_API_KEY}  # 必须设置环境变量
  llm:
    provider: dashscope
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      model: qwen-max
```

#### 决策 8：Docker Compose 多环境支持

**开发环境启动**：
```bash
# 启动所有服务（包括 Config Server）
docker-compose -f docker-compose.yml -f docker-compose.config.yml up -d

# 查看日志
docker-compose logs -f rkos-backend
```

**生产环境启动**：
```bash
# 使用生产配置文件
docker-compose -f docker-compose.yml -f docker-compose.prod.yml -f docker-compose.config.yml up -d

# 设置环境变量
export RKOS_API_KEY=prod-secret-key-xxxxx
export DB_USERNAME=prod_user
export DB_PASSWORD=prod_secret_password
export DASHSCOPE_API_KEY=sk-xxxxxxxxxx
```

**docker-compose.prod.yml**：
```yaml
version: '3.8'

services:
  rkos-backend:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - RKOS_API_KEY=${RKOS_API_KEY}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          cpus: '1.0'
          memory: 1G
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

---

### 决策影响分析

#### 实施顺序

1. **第一阶段：基础框架搭建**（Week 1-2）
   - Spring Boot 4.1 + Spring AI 2.0 项目初始化
   - MongoDB + PostgreSQL 双存储配置
   - API Key 认证 + 参数校验 + 异常处理

2. **第二阶段：Agent 核心实现**（Week 3-4）
   - Story Understanding Agent 实现
   - Prompt 模板管理 + Spring Cloud Config 集成
   - PostgreSQL 记忆持久化

3. **第三阶段：业务逻辑完善**（Week 5-6）
   - 双存储一致性控制（应用层协调 + 补偿机制）
   - 确认反馈接口
   - Docker Compose 多环境配置

4. **第四阶段：测试与优化**（Week 7-8）
   - 单元测试 + 集成测试
   - 性能调优
   - 生产环境部署验证

#### 跨组件依赖

| 组件 | 依赖 | 影响 |
|------|------|------|
| Story Understanding Agent | Prompt 模板服务、ChatClient、记忆仓库 | Prompt 热更新需重启 Config Client |
| StoryPersistenceService | MongoTemplate、JdbcTemplate | 双存储写入失败需补偿机制 |
| ApiKeyAuthInterceptor | 环境变量（RKOS_API_KEY） | 生产环境必须设置环境变量 |
| Config Server | Git 仓库（或本地卷） | Config Repo 变更需手动触发刷新 |

---

### 风险与缓解措施

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Spring AI Alibaba 延迟兼容 2.0 | 无法同时使用 Spring AI 2.0 + Alibaba | 先用 Spring AI 原生 DashScope starter，等 Alibaba 2.0 发布后切换 |
| Config Server 单点故障 | Prompt 热更新不可用 | 客户端缓存最后已知配置，启动时 fail-fast |
| 双存储不一致 | 数据丢失或重复 | 应用层事务 + 补偿机制 + 监控告警 |
| LLM 调用超时 | 用户体验下降 | 设置合理超时（30s），前端显示加载状态 |

## 实现模式与一致性规则

### 命名规范

**Java 代码：**
- 类名：`PascalCase`（如 `StoryUnderstandingAgent`）
- 方法/变量：`camelCase`（如 `getUserById`, `storyContent`）
- 常量：`UPPER_SNAKE_CASE`（如 `MAX_RETRY_COUNT`）
- 包名：`com.rkos.{module}.{submodule}`（全小写）

**数据库（PostgreSQL）：**
- 表名：`snake_case` 复数（如 `users`, `relationship_genomes`）
- 字段名：`snake_case`（如 `user_id`, `created_at`）
- 外键：`{referenced_table}_id`（如 `user_id`）
- 索引：`idx_{table}_{column}`（如 `idx_users_email`）

**API（RESTful）：**
- URL 路径：`kebab-case` 复数（如 `/api/v1/stories`, `/api/v1/user-profiles`）
- JSON 字段：`camelCase`（如 `userId`, `createdAt`）— *需在 DTO 层从 DB 的 `snake_case` 转换*
- 查询参数：`camelCase`（如 `?userId=123&status=active`）

**文件/目录：**
- Java 源文件：`PascalCase.java`（如 `StoryController.java`）
- 配置文件：`application-{profile}.yml`（如 `application-dev.yml`）
- Prompt 模板：`{agent-name}-prompt.txt`（如 `story-understanding-prompt.txt`）
- 测试文件：`{ClassName}Test.java`（如 `StoryServiceTest.java`）

---

### 项目结构模式

采用**分层架构（Layered Architecture）**结合**模块化设计**：

```
rkos-backend/
├── src/main/java/com/rkos/
│   ├── config/                    # 全局配置
│   │   ├── ApiKeyAuthConfig.java
│   │   ├── SpringAiConfig.java
│   │   └── SwaggerConfig.java
│   ├── common/                    # 通用组件
│   │   ├── ApiResponse.java       # 统一响应包装
│   │   ├── RkosException.java     # 自定义异常
│   │   ── GlobalExceptionHandler.java
│   ├── modules/                   # 业务模块
│   │   ├── story/                 # 故事模块
│   │   │   ├── controller/
│   │   │   │   └── StoryController.java
│   │   │   ├── service/
│   │   │   │   ├── StoryService.java
│   │   │   │   └── StoryPersistenceService.java
│   │   │   ├── agent/
│   │   │   │   └── StoryUnderstandingAgent.java
│   │   │   ├── repository/
│   │   │   │   ├── MongoStoryRepository.java
│   │   │   │   └── PostgresGenomeRepository.java
│   │   │   ── dto/
│   │   │       ├── StoryRequest.java
│   │   │       └── StoryResponse.java
│   │   ├── knowledge/             # 知识演化模块（第二迭代）
│   │   │   ├── agent/
│   │   │   │   ├── RelationshipRuleAgent.java
│   │   │   │   └── RelationshipPatternAgent.java
│   │   │   └── service/
│   │   │       └── KnowledgeEvolutionService.java
│   │   ── user/                  # 用户模块（未来）
│   │       └── ...
│   └── RkosApplication.java       # 启动类
├── src/main/resources/
│   ├── prompts/                   # AI Prompt 模板
│   │   ├── story-understanding/
│   │   │   ├── system-prompt.txt
│   │   │   └── user-template.txt
│   │   ├── relationship-rule/
│   │   │   ├── system-prompt.txt
│   │   │   ── user-template.txt
│   │   ── ...
│   ├── application.yml            # 公共配置
│   ├── application-dev.yml        # 开发环境
│   └── application-prod.yml       # 生产环境
├── src/test/java/com/rkos/        # 测试代码
│   ├── modules/story/service/
│   │   ── StoryServiceTest.java
│   └── ...
├── docker-compose.yml             # Docker Compose 配置
├── docker-compose.config.yml      # Config Server 配置
├── docker-compose.prod.yml        # 生产环境覆盖
├── pom.xml                        # Maven 依赖
└── README.md
```

**关键原则：**
1. **按功能模块组织**：每个模块（story, knowledge, user）包含完整的 controller/service/agent/repository/dto
2. **Agent 独立目录**：AI Agent 逻辑放在 `agent/` 子目录，与普通 Service 区分
3. **Prompt 外置管理**：所有 Prompt 模板放在 `src/main/resources/prompts/`，禁止硬编码
4. **测试与源码同结构**：测试代码镜像主代码目录结构

---

### API 响应格式

**统一响应包装器 `ApiResponse<T>`：**

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private String code;          // SUCCESS, VALIDATION_ERROR, INTERNAL_ERROR 等
    private String message;       // 人类可读的消息
    private T data;               // 业务数据，失败时为 null
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "操作成功", data, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(code, message, data, LocalDateTime.now());
    }
}
```

**成功响应示例：**
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "storyId": "abc123",
    "content": "我和她是在图书馆认识的...",
    "createdAt": "2026-07-15T10:30:00Z"
  },
  "timestamp": "2026-07-15T10:30:00Z"
}
```

**错误响应示例：**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "参数校验失败",
  "data": {
    "content": "故事内容不能为空",
    "type": "无效的故事类型"
  },
  "timestamp": "2026-07-15T10:30:00Z"
}
```

**HTTP 状态码映射：**
- `200 OK` → `code: "SUCCESS"`
- `400 Bad Request` → `code: "VALIDATION_ERROR"`
- `401 Unauthorized` → `code: "UNAUTHORIZED"`
- `404 Not Found` → `code: "NOT_FOUND"`
- `500 Internal Server Error` → `code: "INTERNAL_ERROR"`

---

### Agent 调用与 Prompt 管理规范

**1. Prompt 加载规范：**

```java
@Service
public class PromptTemplateService {
    
    @Value("${rkos.prompts.base-path}")
    private String basePath;
    
    /**
     * 加载系统提示词
     * @param agentName Agent 名称（如 story-understanding）
     * @return 系统提示词内容
     */
    public String loadSystemPrompt(String agentName) {
        String path = basePath + agentName + "/system-prompt.txt";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RkosException("PROMPT_LOAD_ERROR", "无法加载 Prompt: " + path, e);
        }
    }
    
    /**
     * 加载用户模板（含变量占位符）
     */
    public String loadUserTemplate(String agentName) {
        String path = basePath + agentName + "/user-template.txt";
        // ... 同上
    }
}
```

**2. Agent 调用规范：**

```java
@Service
public class StoryUnderstandingAgent {
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private PromptTemplateService promptTemplateService;
    
    @Autowired
    private BeanOutputConverter<RelationshipGenome> outputConverter;
    
    public RelationshipGenome understandStory(String storyContent) {
        // 加载 Prompt
        String systemPrompt = promptTemplateService.loadSystemPrompt("story-understanding");
        String userTemplate = promptTemplateService.loadUserTemplate("story-understanding");
        
        // 替换变量
        String userMessage = userTemplate.replace("{{story}}", storyContent);
        
        // 构建 Prompt
        Prompt prompt = new Prompt(
            List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userMessage)
            )
        );
        
        // 调用 LLM（使用结构化输出）
        ChatResponse response = chatClient.call(prompt);
        String jsonOutput = response.getResult().getOutput().getContent();
        
        // 解析为 Java 对象
        return outputConverter.convert(jsonOutput);
    }
}
```

**3. 记忆管理规范：**

```yaml
# application.yml
spring:
  ai:
    chat:
      memory:
        repository:
          type: jdbc
          jdbc:
            table-name: chat_memories
            schema: public
        options:
          max-messages: 10  # 保留最近 10 轮对话
          ttl-days: 7       # 7 天后自动清理
```

**关键原则：**
- ✅ **禁止硬编码 Prompt**：所有 Prompt 必须通过 `PromptTemplateService` 加载
- ✅ **结构化输出**：优先使用 `BeanOutputConverter` 或 JSON Mode，避免解析纯文本
- ✅ **记忆抽象化**：使用 `ChatMemoryRepository` 接口，底层可切换存储介质
- ❌ **禁止在 Service 中直接调用 LLM**：必须通过 Agent 层封装

---

### 异常处理规范

**1. 自定义异常：**

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class RkosException extends RuntimeException {
    
    private String errorCode;
    private Object details;
    
    public RkosException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RkosException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public RkosException(String errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
}
```

**2. 全局异常处理器：**

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage
                ));
        log.warn("参数校验失败: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", "参数校验失败", errors));
    }
    
    /**
     * 自定义业务异常
     */
    @ExceptionHandler(RkosException.class)
    public ResponseEntity<ApiResponse<Object>> handleRkosException(RkosException ex) {
        log.warn("业务异常 [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(getHttpStatus(ex.getErrorCode()))
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage(), ex.getDetails()));
    }
    
    /**
     * 未捕获的通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("未捕获的异常", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "系统内部错误", null));
    }
    
    private HttpStatus getHttpStatus(String errorCode) {
        switch (errorCode) {
            case "UNAUTHORIZED": return HttpStatus.UNAUTHORIZED;
            case "NOT_FOUND": return HttpStatus.NOT_FOUND;
            case "VALIDATION_ERROR": return HttpStatus.BAD_REQUEST;
            default: return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
```

**3. LLM 调用异常特殊处理：**

```java
@Service
public class LlmCallService {
    
    @Retryable(value = {LLMTimeoutException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ChatResponse callWithRetry(Prompt prompt) {
        try {
            return chatClient.call(prompt);
        } catch (ApiException e) {
            if (e.getStatusCode() == 429) {  // 配额不足
                throw new RkosException("LLM_QUOTA_EXCEEDED", "LLM 调用配额已用完", e);
            } else if (e.getStatusCode() == 504) {  // 超时
                throw new LLMTimeoutException("LLM 调用超时", e);
            }
            throw new RkosException("LLM_CALL_FAILED", "LLM 调用失败", e);
        }
    }
}
```

**关键原则：**
- ✅ **所有异常必须转换为 `ApiResponse`**：通过 `@ControllerAdvice` 统一处理
- ✅ **区分业务异常与系统异常**：`RkosException` vs 通用 `Exception`
- ✅ **LLM 调用需重试机制**：使用 `@Retryable` 处理临时故障
- ❌ **禁止返回原始堆栈信息**：生产环境只返回友好错误消息

---

### 强制一致性规则

**所有 AI Agent 在实现 RKOS 项目时必须遵守：**

1. **命名一致性**：严格遵循上述命名规范（Java PascalCase, DB snake_case, API kebab-case）
2. **响应格式一致性**：所有 API 必须返回 `ApiResponse<T>` 包装对象
3. **Prompt 管理一致性**：禁止硬编码 Prompt，必须通过 `PromptTemplateService` 加载
4. **异常处理一致性**：所有异常必须通过 `GlobalExceptionHandler` 转换为 `ApiResponse`
5. **模块组织一致性**：新功能必须放在 `modules/{module-name}/` 下，遵循 controller/service/agent/repository/dto 结构
6. **测试覆盖一致性**：每个 Service 必须有对应的 `*Test.java` 单元测试
7. **配置隔离一致性**：环境特定配置必须放在 `application-{profile}.yml`，禁止硬编码在代码中

**违反规则的后果：**
- 代码审查时会被标记为 "架构违规"
- 可能导致多 Agent 协作时的集成冲突
- 影响项目的可维护性和可扩展性

---

### 正反示例对比

#### ✅ 正确示例

**1. 正确的 Controller 实现：**
```java
@RestController
@RequestMapping("/api/v1/stories")
@Validated
public class StoryController {
    
    @Autowired
    private StoryService storyService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<StoryResponse>> submitStory(
            @Valid @RequestBody StoryRequest request,
            @RequestHeader("X-API-Key") String apiKey) {
        
        StoryResponse response = storyService.submit(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

**2. 正确的 Service 实现：**
```java
@Service
@Transactional
public class StoryService {
    
    @Autowired
    private StoryUnderstandingAgent understandingAgent;
    
    @Autowired
    private StoryPersistenceService persistenceService;
    
    public StoryResponse submit(StoryRequest request) {
        // 1. 调用 Agent 理解故事
        RelationshipGenome genome = understandingAgent.understandStory(request.getContent());
        
        // 2. 持久化到双存储
        Story story = new Story();
        story.setContent(request.getContent());
        story.setGenome(genome);
        persistenceService.saveStoryWithGenome(story, genome);
        
        // 3. 返回响应
        return new StoryResponse(story.getId(), story.getCreatedAt());
    }
}
```

**3. 正确的 Prompt 模板文件：**
```
# src/main/resources/prompts/story-understanding/system-prompt.txt

你是一个关系知识抽取专家。你的任务是从用户的故事中抽取关键的关系信息，
并生成标准化的 Relationship Genome。

请严格按照以下 JSON 格式输出：
{
  "participants": [...],
  "relationship_type": "...",
  "key_events": [...],
  "emotional_tone": "..."
}
```

#### ❌ 错误示例（反模式）

**1. 硬编码 Prompt（禁止）：**
```java
// ❌ 错误：Prompt 硬编码在代码中
String systemPrompt = "你是一个关系知识抽取专家...";
Prompt prompt = new Prompt(new SystemMessage(systemPrompt));
```

**2. 直接返回业务对象（禁止）：**
```java
// ❌ 错误：未使用 ApiResponse 包装
@PostMapping
public StoryResponse submitStory(@RequestBody StoryRequest request) {
    return storyService.submit(request);
}
```

**3. 混合命名风格（禁止）：**
```java
// ❌ 错误：表名使用 PascalCase
@Table(name = "StoryGenomes")  // 应该是 story_genomes
public class StoryGenome { ... }

// ❌ 错误：API 路径使用 snake_case
@RequestMapping("/api/v1/story_genomes")  // 应该是 /api/v1/story-genomes
```

**4. 吞掉异常（禁止）：**
```java
// ❌ 错误：捕获异常但不处理
try {
    chatClient.call(prompt);
} catch (Exception e) {
    // 什么都不做，导致问题被隐藏
}
```

---

###  enforcement 指南

**如何验证规则被遵守：**

1. **代码审查清单**：
   - [ ] 所有 API 返回 `ApiResponse<T>`
   - [ ] 所有 Prompt 从文件加载，无硬编码
   - [ ] 命名符合规范（Java/DB/API）
   - [ ] 异常通过 `@ControllerAdvice` 统一处理
   - [ ] 测试覆盖率 > 80%

2. **自动化检查**：
   - 使用 ArchUnit 编写架构测试，自动检测违规
   - CI/CD 流水线集成代码风格检查（Checkstyle, SpotBugs）

3. **文档化违规**：
   - 发现违规时，在 PR 评论中标记 "ARCH_VIOLATION"
   - 记录到 `docs/architecture-violations.md` 供后续复盘

**规则更新流程：**
1. 提出修改建议（GitHub Issue）
2. 团队讨论并达成共识
3. 更新 `architecture.md` 中的相关章节
4. 通知所有 AI Agent 同步最新规则

## 项目结构与边界

### 完整项目目录树（第一迭代 MVP）

```
rkos-backend/
├── pom.xml                                    # Maven 依赖
├── docker-compose.yml                         # 开发环境（Spring Boot + MongoDB + PostgreSQL）
├── docker-compose.prod.yml                    # 生产环境覆盖
├── docker-compose.config.yml                  # Spring Cloud Config Server
├── Dockerfile
├── .gitignore
├── .env.example
│
├── src/main/java/com/rkos/
│   ├── RkosApplication.java
│   │
│   ├── config/
│   │   ├── MongoConfig.java
│   │   ├── PostgresConfig.java
│   │   ├── SpringAiConfig.java
│   │   ├── ApiKeyAuthConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── RetryConfig.java
│   │
│   ├── common/
│   │   ├── ApiResponse.java
│   │   ├── RkosException.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── LlmCallService.java
│   │   └── PromptTemplateService.java
│   │
│   └── modules/
│       └── story/
│           ├── controller/
│           │   └── StoryController.java
│           ├── service/
│           │   ├── StoryService.java
│           │   └── StoryPersistenceService.java
│           ├── agent/
│           │   └── StoryUnderstandingAgent.java
│           ├── repository/
│           │   ├── MongoStoryRepository.java
│           │   └── PostgresGenomeRepository.java
│           ├── model/
│           │   ├── Story.java
│           │   ├── RelationshipGenome.java
│           │   ├── Relationship.java
│           │   ├── Participant.java
│           │   ├── KeyEvent.java
│           │   ├── CausalChain.java
│           │   ├── ConflictPattern.java
│           │   ├── Outcome.java
│           │   ├── Confidence.java
│           │   └── EmotionalArc.java
│           └── dto/
│               ├── StoryRequest.java
│               ├── StoryResponse.java
│               └── GenomeResponse.java
│
├── src/main/resources/
│   ├── prompts/
│   │   └── story-understanding/
│   │       ├── system-prompt.txt
│   │       └── user-template.txt
│   ├── db/migration/
│   │   └── V1__init_schema.sql
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── bootstrap.yml
│
├── src/test/java/com/rkos/
│   └── modules/story/
│       ├── service/
│       │   └── StoryServiceTest.java
│       └── agent/
│           └── StoryUnderstandingAgentTest.java
│
├── src/test/resources/
│   └── prompts/
│       └── story-understanding/
│           └── system-prompt.txt
│
└── docs/
    ├── api-guide.md
    └── deployment.md
```

### 需求到结构映射

| 需求类别 | 归属模块/目录 | 核心组件 | PRD 编号 |
|----------|--------------|----------|----------|
| 故事管理 | `modules/story` | `StoryController` + `StoryService` + `StoryPersistenceService` | FR1-5 |
| 故事理解与基因组 | `modules/story/agent` | `StoryUnderstandingAgent` + `PromptTemplateService` | FR6-10 |
| 基因组查询 | `modules/story/controller` | `StoryController` 内端点 | FR11-13 |
| API 与认证 | `config` + `common` | `ApiKeyAuthConfig` + `GlobalExceptionHandler` | FR14-15, FR18 |
| 运维可观测 | `common` + `config` | 健康检查端点 + 结构化日志 | FR25-27 |
| 部署可复现 | 根目录 | `docker-compose.yml` + `docs/` | FR29-31 |

### 架构边界

| 边界类型 | 定义 | 实现方式 |
|----------|------|----------|
| API 边界 | `/api/v1/*` RESTful 端点 | `@RequestMapping` 统一前缀 |
| 认证边界 | API Key 拦截器 | `HandlerInterceptor` |
| 双存储边界 | MongoDB ↔ PostgreSQL 数据协调 | `StoryPersistenceService` 应用层协调 |
| Agent 边界 | LLM 调用封装 | `StoryUnderstandingAgent` + `LlmCallService` |
| 配置边界 | 环境隔离 | Spring Profile + Config Server |

### 集成点（第一迭代）

| 集成方向 | 通信方式 | 说明 |
|----------|----------|------|
| Controller → Service | Spring DI 直接调用 | 同进程内 |
| Service → Agent | Spring DI 直接调用 | 同进程内 |
| Agent → LLM | Spring AI `ChatClient` | HTTP 调用外部 LLM API |
| Service → MongoDB | Spring Data MongoDB | `MongoRepository` |
| Service → PostgreSQL | Spring Data JPA | `JpaRepository` |
| Config Client → Config Server | Spring Cloud Config | HTTP，Prompt 热更新 |

---

## 数据模型（第一迭代 MVP）

### 存储策略

采用**混合存储方案**：PostgreSQL `relationship_genomes` 表中，高频查询字段扁平化为关系列（可建 B-tree 索引），复杂嵌套结构统一存入 `genome_data` JSONB 字段。Java 模型类直接映射 JSONB 内部结构，通过序列化/反序列化实现对象与数据库的转换。应用层写入时保证扁平列与 JSONB 内对应字段的一致性。

### MongoDB — `stories` 集合

```json
{
  "_id": "ObjectId",
  "story_id": "story_100001",
  "author_id": "user_001",
  "created_at": "2026-07-15T10:21:00",
  "content": "...原始故事全文...",
  "relationship_type": "情侣",
  "anonymous": true,
  "attachments": [],
  "status": "active",
  "version": 1,
  "processing_status": "completed",
  "processing_metadata": {
    "agent_version": "v1.0",
    "model_used": "gpt-4o",
    "started_at": "2026-07-15T10:21:05",
    "completed_at": "2026-07-15T10:21:42",
    "retry_count": 0,
    "error_message": null
  },
  "content_length": 2340,
  "language": "zh-CN"
}
```

**索引：**
- `story_id`：唯一索引
- `processing_status`：普通索引
- `created_at`：普通索引

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `_id` | ObjectId | MongoDB 主键 |
| `story_id` | String | 业务唯一标识（UUID，同时作为 PostgreSQL 的关联键） |
| `author_id` | String | 作者标识（第一迭代为占位，可为空或 `"anonymous"`） |
| `content` | String | 故事正文 |
| `relationship_type` | String | 关系类型（用户提交时可选） |
| `anonymous` | Boolean | 是否匿名 |
| `attachments` | Array | 附件列表（第一迭代仅支持文字，预留数组） |
| `status` | String | 故事状态：`active` |
| `version` | Integer | 数据版本号 |
| `processing_status` | String | Agent 处理状态：`PENDING` / `PROCESSING` / `COMPLETED` / `FAILED` |
| `processing_metadata` | Object | 处理元信息（Agent 版本、模型、耗时、重试次数） |
| `content_length` | Integer | 故事字数 |
| `language` | String | 语言标识 |

### PostgreSQL — `relationship_genomes` 表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGSERIAL | 主键 |
| `story_id` | VARCHAR(36) UNIQUE NOT NULL | 关联 MongoDB 的业务键（UUID） |
| `agent_version` | VARCHAR(20) | 生成该 Genome 的 Agent 版本 |
| `genome_data` | JSONB | 完整的 Genome 结构化数据 |
| `overall_confidence` | DECIMAL(3,2) | 整体置信度（冗余字段，便于查询过滤） |
| `relationship_type` | VARCHAR(50) | 关系类型（冗余字段，便于过滤） |
| `outcome_type` | VARCHAR(50) | 结果类型（冗余字段，便于过滤） |
| `created_at` | TIMESTAMP DEFAULT NOW() | 生成时间 |
| `updated_at` | TIMESTAMP DEFAULT NOW() | 更新时间 |

**索引：**
- `story_id`：唯一索引
- `relationship_type`：普通索引
- `overall_confidence`：普通索引
- `created_at`：普通索引

**`genome_data` JSONB 内部结构：**

```json
{
  "genome_id": "genome_2031",
  "story_id": "story_100001",
  "version": "v1.0",

  "relationship": {
    "type": "情侣",
    "duration": "3年",
    "stage": "冷淡期",
    "start_context": "大学校园"
  },

  "participants": {
    "A": {
      "role": "叙述者",
      "attachment": "焦虑型",
      "behaviors": ["索取确认", "频繁追问"],
      "emotions": ["焦虑", "不安"],
      "age_at_story": 25,
      "gender": "male"
    },
    "B": {
      "role": "对方",
      "attachment": "回避型",
      "behaviors": ["减少沟通"],
      "emotions": ["压力", "疲惫"],
      "age_at_story": 24,
      "gender": "female"
    }
  },

  "key_events": [
    {
      "event": "工作压力增加",
      "position": "beginning",
      "description": "B 的工作进入高压期"
    },
    {
      "event": "频繁确认",
      "position": "climax",
      "description": "A 开始反复追问 B 是否还爱自己"
    },
    {
      "event": "分手",
      "position": "end",
      "description": "B 提出分手"
    }
  ],

  "causal_chain": [
    "工作压力增加", "陪伴减少", "安全感下降",
    "频繁确认", "沟通恶化", "分手"
  ],

  "conflict_patterns": [
    {
      "type": "communication",
      "frequency": "recurring",
      "resolution": "escalation",
      "description": "沟通频率持续下降"
    },
    {
      "type": "emotional_needs",
      "frequency": "recurring",
      "resolution": "unresolved",
      "description": "A 的情感需求未被识别"
    }
  ],

  "outcome": {
    "type": "分手",
    "initiator": "B",
    "manner": "direct"
  },

  "lessons": ["情绪理解优先于解释"],

  "confidence": {
    "overall": 0.85,
    "relationship": 0.90,
    "participants": 0.82,
    "causal_chain": 0.78,
    "conflict_patterns": 0.80
  },

  "emotional_arc": {
    "dominant_emotions": ["遗憾", "不舍"],
    "trajectory": "decline"
  }
}
```

### PostgreSQL — `chat_memories` 表（Spring AI 框架管理）

| 字段 | 类型 | 说明 |
|------|------|------|
| `conversation_id` | VARCHAR(255) | 对话 ID（对应 storyId） |
| `content` | TEXT | 消息内容 |
| `type` | VARCHAR(20) | 消息类型：`USER` / `ASSISTANT` |
| `timestamp` | TIMESTAMP | 消息时间 |

### PostgreSQL — `api_keys` 表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGSERIAL | 主键 |
| `key_hash` | VARCHAR(64) | API Key 的 SHA-256 哈希 |
| `name` | VARCHAR(100) | 标识名称 |
| `is_active` | BOOLEAN | 是否启用 |
| `created_at` | TIMESTAMP | 创建时间 |
| `expires_at` | TIMESTAMP | 过期时间（可为空） |

### 跨数据库关联策略

- MongoDB `stories.story_id` 与 PostgreSQL `relationship_genomes.story_id` 通过 **UUID 字符串桥接**
- **不设强外键约束**（跨数据库无法实现），保证系统松耦合
- 一致性由 `StoryPersistenceService` 在应用层协调（先写 MongoDB 后写 PostgreSQL，失败走补偿机制）

---

## 架构验证结果

### 一致性验证 ✅

| 验证项 | 结果 | 说明 |
|----------|------|------|
| 技术栈兼容性 | ✅ 通过 | Spring Boot 4.1.0 + Spring AI 2.0.0 + Java 21 LTS，版本完全兼容 |
| 双存储协调 | ✅ 通过 | MongoDB + PostgreSQL 通过应用层 `StoryPersistenceService` 协调，UUID 桥接 |
| 命名规范一致性 | ✅ 通过 | Java PascalCase / DB snake_case / API kebab-case / JSON camelCase 全覆盖 |
| Prompt 管理 | ✅ 通过 | 文件外置 + `PromptTemplateService` 加载 + Config Server 热更新 |
| 异常处理链路 | ✅ 通过 | 自定义异常 → `@ControllerAdvice` → `ApiResponse<T>` 统一包装 |
| 数据存储策略 | ✅ 通过 | 混合存储：高频字段扁平化 + 嵌套结构 JSONB，兼顾查询与灵活性 |
| 通信模式 | ✅ 通过 | Spring DI 同进程调用 + HTTP 外部调用，边界清晰 |

### 需求覆盖验证 ✅

**功能需求覆盖（第一迭代 MVP）：**

| PRD 需求 | 架构支撑 | 状态 |
|----------|----------|------|
| FR1-5 故事管理 | `StoryController` + `StoryService` + `StoryPersistenceService` + MongoDB `stories` 集合 | ✅ 覆盖 |
| FR6-10 故事理解与基因组 | `StoryUnderstandingAgent` + `PromptTemplateService` + `LlmCallService` | ✅ 覆盖 |
| FR11-13 基因组查询 | `PostgresGenomeRepository` + `relationship_genomes` 表 + 冗余查询列 | ✅ 覆盖 |
| FR14-15 API 与认证 | `ApiKeyAuthConfig` + `api_keys` 表 + `@RequestMapping("/api/v1/*")` | ✅ 覆盖 |
| FR16-17 限流熔断 | **明确排除**（用户决策：第一迭代不做限流） | ✅ 符合决策 |
| FR18 API 版本管理 | `/api/v1/` 前缀 + `SwaggerConfig` | ✅ 覆盖 |
| FR19-21 内容安全 | **延至第二迭代** | ✅ 符合决策 |
| FR22-24 数据隐私 | **延至第二迭代** | ✅ 符合决策 |
| FR25-27 运维可观测 | 健康检查端点 + 结构化日志 + Prompt 配置外置 | ✅ 覆盖 |
| FR28 重新处理 | `POST /api/v1/stories/{id}/reprocess` 端点 | ✅ 覆盖 |
| FR29-31 部署可复现 | `docker-compose.yml` + `docker-compose.prod.yml` + `docs/` | ✅ 覆盖 |

**非功能需求覆盖：**

| PRD 需求 | 架构支撑 | 状态 |
|----------|----------|------|
| NFR1-4 性能 | 异步 LLM 处理 + 合理超时（30s）+ 异步补偿机制 | ✅ 覆盖 |
| NFR5-6 安全 | HTTPS（部署层）+ API Key 哈希存储 + 最小权限 | ✅ 覆盖 |
| NFR10 可扩展 | 无状态设计，支持多实例横向扩展 | ✅ 覆盖 |
| NFR12/15 LLM 可插拔 | Spring AI `ChatClient` 抽象层 + 多提供商配置 | ✅ 覆盖 |
| NFR17 LLM 重试 | `LlmCallService` + `@Retryable` + 指数退避 | ✅ 覆盖 |
| NFR20 Prompt 热更新 | Spring Cloud Config Server + `@RefreshScope` | ✅ 覆盖 |
| NFR21 双数据库 | MongoDB + PostgreSQL + 应用层协调 | ✅ 覆盖 |

### 实现就绪度验证 ✅

| 验证项 | 结果 | 说明 |
|----------|------|------|
| 决策完整度 | ✅ | 8 个核心决策均有版本号、代码示例、影响分析 |
| 项目结构完整度 | ✅ | 完整目录树 + 每个文件职责明确 |
| 数据模型完整度 | ✅ | MongoDB 集合结构 + PostgreSQL 表结构 + JSONB 内部完整结构 |
| 命名规范完整度 | ✅ | 4 种命名风格全覆盖 + 正反示例对比 |
| 一致性规则完整度 | ✅ | 7 条强制规则 + 违规后果说明 |
| 异常处理完整度 | ✅ | 自定义异常 + 全局处理 + LLM 特殊处理 + 重试策略 |
| API 响应格式完整度 | ✅ | 统一 `ApiResponse<T>` 包装 + 分页规范 |
| Agent 调用规范完整度 | ✅ | Prompt 外置 + `BeanOutputConverter` 序列化 + 重试机制 |

### 缺口分析

**关键缺口：无**

**重要缺口：无**

**次要缺口（可选优化）：**
1. `V1__init_schema.sql` Flyway 迁移脚本具体内容未定义（实现时填充即可）
2. `application.yml` 具体配置值未完整列出（实现时参考架构决策填充即可）
3. 未定义日志格式规范（建议实现时统一使用 JSON 结构化日志）

### 架构就绪度评估

**整体状态：可以进入实现阶段**

**置信度：高**

**核心优势：**
- 双存储职责明确，JSONB 混合方案兼顾查询性能与数据灵活性
- Spring AI 原生集成，LLM 可插拔设计成熟
- 强制一致性规则 + 正反示例，多 Agent 协作时不会产生冲突
- MVP 范围严格控制，单人开发可行性高
- 数据模型经过充分讨论，结构清晰且预留扩展空间

**后续迭代增强方向：**
- 第二迭代：新增 `knowledge` 模块、Milvus 向量检索、内容安全过滤
- 第三迭代：`user` 模块、知识图谱 Neo4j、反馈闭环
- 未来：SDK 开放、多模态分析、实时对话能力

### 实现交接指南

**AI Agent 开发准则：**
- 严格遵循所有架构决策中记录的版本号和依赖
- 使用实现模式中的示例代码作为标准参考
- 尊重项目结构边界，不随意创建未定义的包或目录
- 所有 API 响应使用统一 `ApiResponse<T>` 包装
- 所有异常通过 `GlobalExceptionHandler` 统一处理
- LLM 调用统一走 `LlmCallService`，不直接注入 `ChatClient`
- Prompt 模板外置到 `resources/prompts/`，不硬编码
- MongoDB 写入原始数据，PostgreSQL 写入结构化数据，职责不可混淆

**第一个实现步骤：**
创建 Spring Boot 项目脚手架，包括 `pom.xml` 依赖配置、`application.yml` 多环境配置、`docker-compose.yml` 开发环境、`StoryController` 基础框架、MongoDB/PostgreSQL 连接配置，验证双数据库连通性后进入核心功能开发。
