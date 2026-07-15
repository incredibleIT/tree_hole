---
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - step-04-final-validation
inputDocuments:
  - _bmad-output/prd.md
  - _bmad-output/architecture.md
---

# RKOS（关系知识操作系统）- Epic 拆分

## 概述

本文档提供 RKOS 第一迭代 MVP 的完整 Epic 和 Story 拆分，将 PRD 需求、架构决策分解为可实现的开发任务。

## 需求清单

### 功能需求

- FR1：故事贡献者可以通过 API 提交一段文字故事
- FR2：系统可以在接收到故事后存储原始内容（非结构化）
- FR3：故事贡献者可以通过故事标识查询已提交的故事详情
- FR4：系统可以对已存储的故事进行重新处理（运维操作）
- FR5：系统维护者可以按条件过滤和查询故事列表
- FR6：系统可以通过 Agent 从故事中抽取人物关系、时间线、冲突类型、情感模式等结构化特征
- FR7：系统可以将抽取结果生成为标准化的关系基因组（Genome）
- FR8：系统可以为 Genome 的各维度标注置信度
- FR9：系统可以在信息不充分时生成部分 Genome 并标注低置信度
- FR10：系统可以在处理完成后返回确认摘要，展示抽取到的关键信息
- FR11：系统可以持久化存储结构化的 Genome 数据
- FR12：外部调用者可以通过 API 查询 Genome 列表（支持分页和过滤）
- FR13：外部调用者可以通过故事标识查询对应的 Genome
- FR14：系统可以通过 RESTful API 暴露所有核心能力
- FR15：系统可以通过 API Key 进行基本的身份验证
- FR18：系统可以支持 API 版本管理
- FR25：系统可以提供健康检查端点
- FR26：系统可以记录结构化的操作日志
- FR27：系统维护者可以调整 Agent 的 Prompt 配置
- FR28：系统维护者可以对指定故事触发重新处理流程
- FR29：系统可以通过一键命令启动完整的开发/演示环境
- FR30：系统可以提供完整的部署文档和使用说明
- FR31：系统可以提供 API 文档和使用示例

### 非功能需求

- NFR1：故事提交 API（不含 LLM 处理）响应时间 ≤ 2 秒
- NFR2：Agent 端到端处理（故事接收到 Genome 生成）≤ 60 秒
- NFR3：Genome 查询 API 响应时间 ≤ 500 毫秒
- NFR4：支持至少 10 个并发故事处理请求
- NFR5：所有 API 通信使用 HTTPS 加密传输
- NFR6：API Key 通过安全方式存储（哈希），支持轮换
- NFR10：架构支持从单机平滑迁移到多实例（无状态设计）
- NFR11：支持 10,000 条故事/Genome 规模
- NFR12：LLM 调用支持可配置的模型提供商切换
- NFR13：通过标准 REST API 与外部系统集成
- NFR14：Docker Compose 一键启动零配置
- NFR15：LLM 接口抽象为可插拔服务层
- NFR16：端到端故事处理成功率 ≥ 90%
- NFR17：LLM 调用失败时自动重试（最多 3 次）
- NFR18：健康检查端点返回所有依赖组件状态
- NFR20：Prompt 配置外置，支持运行时调整
- NFR21：API 自动化测试覆盖率 ≥ 80%（贯穿所有 Epic）

### 架构附加需求

- AR1：Spring Boot 4.1.0 + Spring AI 2.0.0 项目初始化（Java 21 LTS，Maven 构建）
- AR2：MongoDB + PostgreSQL 双存储配置与连接管理
- AR3：Flyway 数据库迁移脚本（V1__init_schema.sql）
- AR4：Spring Cloud Config Server 部署与 Prompt 热更新集成
- AR5：统一响应格式 ApiResponse<T> + GlobalExceptionHandler 全局异常处理
- AR6：LlmCallService LLM 调用封装（含 @Retryable 重试机制）
- AR7：PromptTemplateService Prompt 模板加载服务（禁止硬编码）
- AR8：Docker Compose 多环境配置（dev/prod/config-server）
- AR9：Swagger/OpenAPI 文档自动生成

### UX 设计需求

第一迭代为 API 优先（无前端 UI），无 UX 设计需求。

### FR 覆盖映射表

| FR | Epic | 说明 |
|----|------|------|
| FR1 | Epic 1 | 提交故事 API |
| FR2 | Epic 1 | 存储原始内容 |
| FR3 | Epic 1 | 按 ID 查询故事 |
| FR4 | Epic 2 | 重新处理故事 |
| FR5 | Epic 1 | 条件过滤查询故事 |
| FR6 | Epic 2 | Agent 抽取结构化特征 |
| FR7 | Epic 2 | 生成标准化 Genome |
| FR8 | Epic 2 | 标注置信度 |
| FR9 | Epic 2 | 部分 Genome + 低置信度 |
| FR10 | Epic 2 | 返回处理确认摘要 |
| FR11 | Epic 2 | Genome 持久化存储 |
| FR12 | Epic 3 | Genome 列表查询（分页+过滤） |
| FR13 | Epic 3 | 按故事 ID 查询 Genome |
| FR14 | Epic 1 | RESTful API 暴露 |
| FR15 | Epic 1 | API Key 认证 |
| FR18 | Epic 1 | API 版本管理 |
| FR25 | Epic 4 | 健康检查端点 |
| FR26 | Epic 4 | 结构化日志 |
| FR27 | Epic 4 | Prompt 配置调整 |
| FR28 | Epic 2 | 触发重新处理 |
| FR29 | Epic 4 | 一键启动环境 |
| FR30 | Epic 4 | 部署文档 |
| FR31 | Epic 4 | API 文档和示例 |

## Epic 列表

### Epic 1：基础设施与故事提交
系统可启动运行，API 调用者可以提交故事并查询原始内容，所有接口有统一响应格式和基础认证。Story 粒度必须细拆，每个 Story 独立可交付。
**覆盖需求：** FR1, FR2, FR3, FR5, FR14, FR15, FR18 + AR1-5, AR8, AR9 + NFR1, NFR5, NFR6, NFR14

### Epic 2：故事理解 Agent 与基因组生成（核心 Epic）
提交故事后，系统自动调用 Agent 分析内容，生成标准化 Genome 并持久化，支持重新处理。包含测试数据种子（seed data）设计。
**覆盖需求：** FR4, FR6, FR7, FR8, FR9, FR10, FR11, FR28 + AR6, AR7 + NFR2, NFR4, NFR12, NFR15, NFR16, NFR17

### Epic 3：基因组查询
外部调用者可以按条件过滤查询 Genome 列表，支持分页。纯查询层，不包含重新处理逻辑。
**覆盖需求：** FR12, FR13 + NFR3

### Epic 4：运维可观测性与部署
系统维护者可监控健康状态、查看日志、调整 Prompt，拥有一键部署环境。NFR21 聚焦端到端集成测试。
**覆盖需求：** FR25, FR26, FR27, FR29, FR30, FR31 + AR4 + NFR18, NFR20, NFR21（集成测试部分）

<!-- 以下逐个 Epic 展开 Story -->

## Epic 1：基础设施与故事提交

系统可启动运行，API 调用者可以提交故事并查询原始内容，所有接口有统一响应格式和基础认证。

**覆盖需求：** FR1, FR2, FR3, FR5, FR14, FR15, FR18 + AR1-5, AR8, AR9 + NFR1, NFR5, NFR6, NFR14

### Story 1.1：项目骨架初始化

作为**开发者**，
我希望有一个可运行的 Spring Boot 项目骨架，
以便在此基础上进行功能开发。

**验收标准：**

**Given** 项目根目录存在 `pom.xml`
**When** 执行 `mvn clean compile`
**Then** 编译成功，无错误
**And** `RkosApplication.java` 可正常启动（Spring Boot 4.1.0 + Java 21）
**And** `application.yml` 包含 dev/prod profile 基础配置

### Story 1.2：双数据库配置与迁移脚本

作为**开发者**，
我希望 MongoDB 和 PostgreSQL 连接已配置，数据库表结构通过 Flyway 自动迁移，
以便系统可以正常读写数据。

**验收标准：**

**Given** Docker Compose 中 MongoDB 和 PostgreSQL 容器已启动
**When** 应用启动
**Then** MongoDB 连接成功（`MongoConfig.java`）
**And** PostgreSQL 连接成功（`PostgresConfig.java`）
**And** Flyway 自动执行 `V1__init_schema.sql`，创建 `relationship_genomes`、`chat_memories`、`api_keys` 三张表
**And** 索引按设计创建（story_id 唯一索引、relationship_type 普通索引等）

### Story 1.3：统一响应格式与全局异常处理

作为**API 调用者**，
我希望所有 API 返回统一的响应格式，
以便我能一致地解析成功和错误响应。

**验收标准：**

**Given** `ApiResponse<T>` 已实现（包含 code、message、data、timestamp 字段）
**When** 任何 API 返回成功或抛出异常
**Then** 响应体统一使用 `ApiResponse<T>` 包装
**And** `GlobalExceptionHandler` 捕获 `RkosException`、`MethodArgumentNotValidException`、通用 `Exception`
**And** 错误响应包含正确的 HTTP 状态码和业务错误码
**And** 编写单元测试覆盖主要异常场景

### Story 1.4：故事领域模型与 MongoDB 存储

作为**开发者**，
我希望 Story 领域模型和 MongoDB Repository 已实现，
以便故事数据可以持久化到 MongoDB。

**验收标准：**

**Given** `Story.java` 包含所有字段（story_id、author_id、content、relationship_type、anonymous、status、processing_status、processing_metadata 等）
**When** 调用 `MongoStoryRepository.save(story)`
**Then** 故事数据写入 MongoDB `stories` 集合
**And** `story_id` 字段有唯一索引
**And** `processing_status` 和 `created_at` 字段有普通索引
**And** 编写集成测试验证 CRUD 操作

### Story 1.5：故事提交 API

作为**故事贡献者**，
我希望通过 API 提交一段文字故事，
以便系统接收并存储我的故事内容。

**验收标准：**

**Given** API Key 认证已通过
**When** 发送 `POST /api/v1/stories` 请求，Body 包含 content（必填）、relationship_type、anonymous 字段
**Then** 返回 HTTP 201，响应体包含 story_id 和 created_at
**And** 故事数据已写入 MongoDB
**And** content 为空时返回 400 参数校验错误
**And** 响应时间 ≤ 2 秒（NFR1）
**And** `StoryRequest.java` DTO 使用 `@NotBlank` 等 Bean Validation 注解

### Story 1.6：故事查询 API

作为**故事贡献者/系统维护者**，
我希望通过故事 ID 查询已提交的故事详情，并能按条件过滤故事列表，
以便查看和管理已提交的故事。

**验收标准：**

**Given** MongoDB 中存在已提交的故事数据
**When** 发送 `GET /api/v1/stories/{storyId}`
**Then** 返回该故事的完整详情（`StoryResponse` DTO）
**And** storyId 不存在时返回 404
**When** 发送 `GET /api/v1/stories?relationship_type=情侣&processing_status=completed&page=0&size=20`
**Then** 返回符合条件的分页故事列表
**And** 响应体包含 total_count、page、size 分页信息

### Story 1.7：API Key 认证

作为**系统维护者**，
我希望所有 API 端点有基础的 API Key 认证保护，
以便防止未授权访问。

**验收标准：**

**Given** `api_keys` 表中存在有效的 API Key 记录（key_hash 存储）
**When** 请求 Header 包含有效的 `X-API-Key`
**Then** 请求正常通过
**When** 请求 Header 缺少 `X-API-Key` 或 Key 无效
**Then** 返回 HTTP 401 Unauthorized
**And** API Key 以哈希方式存储（NFR6），不存明文
**And** `/api/v1/health` 端点不受认证保护（公开访问）

### Story 1.8：API 版本管理与 Swagger 文档

作为**外部调用者**，
我希望有完整的 API 文档可以浏览和测试，
以便快速了解接口规格。

**验收标准：**

**Given** `SwaggerConfig.java` 已配置
**When** 访问 `/swagger-ui.html` 或 `/api-docs`
**Then** 可看到所有已实现的 API 端点文档
**And** 所有 API 路径以 `/api/v1/` 为前缀（FR18）
**And** 文档中包含请求/响应示例和字段说明

### Story 1.9：Docker Compose 开发环境

作为**开发者**，
我希望一键启动完整的开发环境（应用 + MongoDB + PostgreSQL），
以便本地开发和调试。

**验收标准：**

**Given** 项目根目录存在 `docker-compose.yml` 和 `.env.example`
**When** 执行 `docker-compose up`
**Then** Spring Boot 应用、MongoDB、PostgreSQL 三个容器正常启动
**And** 应用容器连接到 MongoDB 和 PostgreSQL
**And** `.env.example` 包含所有必要的环境变量说明
**And** `Dockerfile` 使用 Java 21 基础镜像

## Epic 2：故事理解 Agent 与基因组生成（核心 Epic）

提交故事后，系统自动调用 Agent 分析内容，生成标准化 Genome 并持久化，支持重新处理。包含测试数据种子设计。

**覆盖需求：** FR4, FR6, FR7, FR8, FR9, FR10, FR11, FR28 + AR6, AR7 + NFR2, NFR4, NFR12, NFR15, NFR16, NFR17

### Story 2.1：LLM 调用封装服务

作为**开发者**，
我希望有一个统一的 LLM 调用封装层，
以便所有 Agent 都通过同一接口调用大模型，并自动处理重试和提供商切换。

**验收标准：**

**Given** `LlmCallService.java` 已实现
**When** 调用 LLM 接口
**Then** 通过 Spring AI 2.0.0 的 `ChatClient` 发送请求
**And** 失败时自动重试最多 3 次（`@Retryable`）
**And** 模型提供商可通过 `application.yml` 配置切换（NFR12）
**And** LLM 接口抽象为可插拔服务层（NFR15）
**And** 调用超时和异常有结构化日志记录

### Story 2.2：Prompt 模板加载服务

作为**开发者**，
我希望 Prompt 模板从外置文件加载而非硬编码，
以便在不修改代码的情况下调整 Agent 行为。

**验收标准：**

**Given** `PromptTemplateService.java` 已实现
**When** 请求加载指定 Prompt 模板
**Then** 从 `src/main/resources/prompts/` 目录读取对应的 `.txt` 文件
**And** 支持模板变量替换（如 `{story_content}`、`{relationship_type}`）
**And** 模板文件不存在时抛出明确异常
**And** `story-understanding/system-prompt.txt` 和 `user-template.txt` 已创建

### Story 2.3：Genome 数据模型与 PostgreSQL 存储

作为**开发者**，
我希望 Genome 相关的 Java 数据模型和 PostgreSQL Repository 已实现，
以便 Genome 数据可以序列化/反序列化和持久化。

**验收标准：**

**Given** `model/` 目录下 10 个 Java 类已创建：`RelationshipGenome`、`Relationship`、`Participant`、`KeyEvent`、`CausalChain`、`ConflictPattern`、`Outcome`、`Confidence`、`EmotionalArc`
**And** `PostgresGenomeRepository.java` 已实现
**When** 调用 `save()` 保存 Genome 数据
**Then** `genome_data` JSONB 字段正确写入完整的 Genome 结构
**And** 扁平化列（`relationship_type`、`outcome_type`、`overall_confidence`）同步更新
**And** 读取时 JSONB 正确反序列化为 Java 对象
**And** 编写集成测试验证 JSONB 读写

### Story 2.4：StoryUnderstandingAgent 核心实现

作为**系统维护者**，
我希望有一个 Agent 能从故事中自动抽取结构化关系特征，
以便生成标准化的关系基因组。

**验收标准：**

**Given** `StoryUnderstandingAgent.java` 已实现
**When** 传入一段故事文本调用 Agent
**Then** Agent 通过 `LlmCallService` 发送结构化 Prompt
**And** 使用 `BeanOutputConverter` 将 LLM 输出解析为 `RelationshipGenome` 对象
**And** 输出包含 9 个维度：relationship、participants、key_events、causal_chain、conflict_patterns、outcome、lessons、confidence、emotional_arc
**And** 各维度标注置信度（0.00-1.00）（FR8）
**And** 信息不充分时生成部分 Genome 并标注低置信度（FR9）
**And** 端到端处理时间 ≤ 60 秒（NFR2）

### Story 2.5：Genome 持久化与处理状态管理

作为**系统维护者**，
我希望 Agent 生成的 Genome 自动持久化到 PostgreSQL，同时更新 MongoDB 中的处理状态，
以便追踪每个故事的处理进度。

**验收标准：**

**Given** Agent 处理完成
**When** 调用 `StoryPersistenceService` 保存结果
**Then** Genome 数据写入 PostgreSQL `relationship_genomes` 表
**And** MongoDB 故事的 `processing_status` 更新为 `completed`
**And** `processing_metadata` 记录 agent_version、model_used、started_at、completed_at
**And** 处理失败时 `processing_status` 更新为 `failed`，`error_message` 记录原因
**And** MongoDB 和 PostgreSQL 的写入通过应用层协调，失败走补偿逻辑

### Story 2.6：故事提交触发 Agent 异步处理

作为**故事贡献者**，
我希望提交故事后系统自动开始分析处理，并返回处理确认摘要，
以便知道我的故事正在被处理。

**验收标准：**

**Given** `POST /api/v1/stories` 提交成功
**When** 故事数据写入 MongoDB 后
**Then** 异步触发 `StoryUnderstandingAgent` 开始分析
**And** API 立即返回 201 响应，包含 `processing_status: "processing"`
**And** 处理完成后 `GET /api/v1/stories/{id}` 返回 `processing_status: "completed"` 和确认摘要（FR10）
**And** 确认摘要包含抽取到的关系类型、参与者数量、关键事件数量
**And** 支持至少 10 个并发故事处理（NFR4）
**And** 端到端处理成功率 ≥ 90%（NFR16）

### Story 2.7：故事重新处理

作为**系统维护者**，
我希望对指定故事触发重新处理流程，
以便在 Agent 升级或处理失败后重新生成 Genome。

**验收标准：**

**Given** MongoDB 中存在指定 story_id 的故事
**When** 发送 `POST /api/v1/stories/{storyId}/reprocess`
**Then** 系统重新触发 Agent 分析该故事
**And** 返回 202 Accepted，包含 `processing_status: "reprocessing"`
**And** 新的 Genome 覆盖旧的（PostgreSQL `updated_at` 更新）
**And** storyId 不存在时返回 404
**And** 处理中的故事不允许重复触发（返回 409 Conflict）

### Story 2.8：测试数据种子

作为**开发者/测试者**，
我希望有预设的测试数据种子，
以便在不依赖 LLM 的情况下测试 Genome 查询和持久化流程。

**验收标准：**

**Given** `src/test/resources/` 下存在种子数据文件
**When** 运行集成测试时
**Then** 自动加载 3-5 条预设 Genome 数据到 PostgreSQL
**And** 种子数据覆盖不同 relationship_type（情侣、友谊、家庭）
**And** 种子数据覆盖不同 outcome_type（分手、和好、持续）
**And** 对应的 MongoDB 故事数据同步加载
**And** 种子数据仅用于测试环境，不影响生产

## Epic 3：基因组查询

外部调用者可以按条件过滤查询 Genome 列表，支持分页。纯查询层，不包含重新处理逻辑。

**覆盖需求：** FR12, FR13 + NFR3

### Story 3.1：Genome 列表查询 API

作为**外部调用者**，
我希望通过 API 查询 Genome 列表，支持分页和条件过滤，
以便获取和分析已有的关系基因组数据。

**验收标准：**

**Given** PostgreSQL 中存在 Genome 数据（可通过种子数据加载）
**When** 发送 `GET /api/v1/genomes?page=0&size=20`
**Then** 返回分页 Genome 列表，响应包含 total_count、page、size
**When** 发送 `GET /api/v1/genomes?relationship_type=情侣&outcome_type=分手`
**Then** 返回符合条件的过滤结果
**And** 查询响应时间 ≤ 500 毫秒（NFR3）
**And** 响应使用统一 `ApiResponse<T>` 格式

### Story 3.2：按故事 ID 查询 Genome

作为**外部调用者**，
我希望通过故事标识查询对应的 Genome，
以便查看特定故事的结构化分析结果。

**验收标准：**

**Given** PostgreSQL 中存在指定 story_id 的 Genome
**When** 发送 `GET /api/v1/stories/{storyId}/genome`
**Then** 返回该故事对应的完整 Genome 数据
**And** storyId 不存在时返回 404
**And** 响应使用统一 `ApiResponse<T>` 格式
**And** 响应包含 Genome 的所有 9 个维度数据

## Epic 4：运维可观测性与部署

系统维护者可监控健康状态、查看日志、调整 Prompt，拥有一键部署环境。NFR21 聚焦端到端集成测试。

**覆盖需求：** FR25, FR26, FR27, FR29, FR30, FR31 + AR4 + NFR18, NFR20, NFR21（集成测试部分）

### Story 4.1：健康检查端点

作为**系统维护者**，
我希望有一个健康检查端点，返回所有依赖组件的状态，
以便快速判断系统是否正常运行。

**验收标准：**

**Given** 应用已启动
**When** 发送 `GET /api/v1/health`（无需认证）
**Then** 返回 Spring Boot 应用状态、MongoDB 连接状态、PostgreSQL 连接状态、LLM 服务可用状态
**And** 所有组件正常时返回 HTTP 200
**And** 任一组件异常时返回 HTTP 503，并标明故障组件

### Story 4.2：结构化日志

作为**系统维护者**，
我希望系统记录结构化的操作日志，
以便排查问题和审计操作。

**验收标准：**

**Given** 应用运行中
**When** 发生关键操作（故事提交、Agent 处理、Genome 生成、重新处理）
**Then** 日志以 JSON 格式输出，包含 timestamp、level、operation、story_id、duration_ms
**And** LLM 调用日志包含 model_used、token_count、retry_count
**And** 异常日志包含完整堆栈信息

### Story 4.3：Spring Cloud Config Server 与 Prompt 热更新

作为**系统维护者**，
我希望可以调整 Agent 的 Prompt 配置而无需重启服务，
以便快速迭代 Prompt 效果。

**验收标准：**

**Given** Spring Cloud Config Server 已部署（`docker-compose.config.yml`）
**When** 修改 Prompt 配置文件
**Then** 应用通过 `/actuator/refresh` 端点热加载新配置（NFR20）
**And** 新提交的故事使用更新后的 Prompt
**And** 正在处理的故事不受影响

### Story 4.4：生产环境 Docker Compose 配置

作为**运维人员**，
我希望有完整的生产环境部署配置，
以便一键部署到生产服务器。

**验收标准：**

**Given** `docker-compose.prod.yml` 和 `docker-compose.config.yml` 已创建
**When** 执行生产环境启动命令
**Then** 所有服务以生产配置启动（资源限制、日志轮转、重启策略）
**And** `.env.example` 包含生产环境所有变量说明
**And** Config Server 正常提供配置服务

### Story 4.5：API 文档完善与部署文档

作为**外部调用者/运维人员**，
我希望有完整的 API 使用示例和部署指南，
以便快速集成和部署系统。

**验收标准：**

**Given** `docs/api-guide.md` 已编写
**When** 查阅 API 文档
**Then** 包含所有端点的请求/响应示例、错误码说明、认证方式
**Given** `docs/deployment.md` 已编写
**When** 查阅部署文档
**Then** 包含本地开发、Docker 部署、生产部署的完整步骤
**And** 包含环境变量配置说明和故障排查指南

### Story 4.6：端到端集成测试

作为**开发者**，
我希望有完整的端到端集成测试，
以便验证整个系统的核心流程正常工作。

**验收标准：**

**Given** 测试环境已启动（Docker Compose）
**When** 运行集成测试套件
**Then** 覆盖核心流程：提交故事 → Agent 处理 → Genome 生成 → 查询 Genome
**And** 覆盖重新处理流程
**And** 覆盖认证失败、参数校验、资源不存在等异常场景
**And** API 自动化测试覆盖率 ≥ 80%（NFR21）
