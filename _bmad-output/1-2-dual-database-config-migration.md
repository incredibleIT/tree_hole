# Story 1.2：双数据库配置与迁移脚本

Status: done

## Story

作为**开发者**，
我希望 MongoDB 和 PostgreSQL 连接已配置，数据库表结构通过 Flyway 自动迁移，
以便系统可以正常读写数据。

## Acceptance Criteria

1. **Given** MongoDB 和 PostgreSQL 服务已运行（Docker 容器）
   **When** 应用启动
   **Then** MongoDB 连接成功（Spring Data MongoDB 自动配置）
   **And** PostgreSQL 连接成功（MyBatis-Plus + Spring Boot DataSource 自动配置）
   **And** Flyway 自动执行 `V1__init_schema.sql`，创建 `relationship_genomes`、`chat_memories`、`api_keys` 三张表
   **And** 索引按设计创建（story_id 唯一索引、relationship_type 普通索引等）
   **And** 应用日志显示双数据库连接成功和 Flyway 迁移完成

## Tasks / Subtasks

- [x] Task 1：移除启动类数据库排除策略（AC: #1）
  - [x] Subtask 1.1：移除 `RkosApplication.java` 的 `@SpringBootApplication(exclude = {...})` 中 4 个排除类
  - [x] Subtask 1.2：验证移除后 import 语句无残留（4 个 AutoConfiguration import 删除）
- [x] Task 2：创建 `V1__init_schema.sql` Flyway 迁移脚本（AC: #1）
  - [x] Subtask 2.1：创建 `relationship_genomes` 表（含所有字段、约束、索引）
  - [x] Subtask 2.2：创建 `chat_memories` 表（含 conversation_id 索引）
  - [x] Subtask 2.3：创建 `api_keys` 表（含 key_hash 索引）
- [x] Task 3：配置 PostgreSQL 连接池（AC: #1）
  - [x] Subtask 3.1：在 `application-dev.yml` 中配置 HikariCP 连接池参数
  - [x] Subtask 3.2：在 `application-prod.yml` 中配置生产级连接池参数
- [x] Task 4：验证双数据库连通性（AC: #1）
  - [x] Subtask 4.1：启动 Docker 容器（MongoDB + PostgreSQL）— 已执行
  - [x] Subtask 4.2：执行 `mvn spring-boot:run` 确认应用启动无报错 — Started in 1.837s
  - [x] Subtask 4.3：确认 Flyway 迁移日志输出 — "Successfully applied 1 migration to schema 'public', now at version v1"
  - [x] Subtask 4.4：确认 MongoDB 连接成功日志 — Monitor thread connected, state=CONNECTED
  - [x] Subtask 4.5：通过 `psql` 验证三张表已创建 — 3 表 + flyway_schema_history + 11 索引全部确认
- [x] Task 5：可选 — 创建 `MongoConfig.java` 和 `PostgresConfig.java` 手动配置类（AC: #1）
  - [x] Subtask 5.1：评估是否需要手动配置 — 结论：Spring Boot 4.1 自动配置已足够，无需手动配置类
  - [x] Subtask 5.2：如需自定义连接参数或连接池，创建对应配置类 — 不需要，已跳过

## Dev Notes

### 关键技术决策

1. **优先使用 Spring Boot 自动配置**：MongoDB 和 PostgreSQL 连接优先使用 `spring.data.mongodb.*` 和 `spring.datasource.*` 配置，不创建冗余的手动配置类。仅在自动配置无法满足需求时才创建 `MongoConfig.java` / `PostgresConfig.java`。
2. **MyBatis-Plus DataSource 复用**：MyBatis-Plus 自动复用 Spring Boot 配置的 `DataSource`，无需额外配置 `SqlSessionFactory`。
3. **Flyway 版本评估**：Story 1.1 代码审查中 deferred 项提到 Flyway 12.4.0 手动指定覆盖 BOM 管理版本。本 Story 应评估是否改用 BOM 管理的版本。

### 数据库表结构（Flyway 迁移脚本）

#### `relationship_genomes` 表

```sql
CREATE TABLE relationship_genomes (
    id              BIGSERIAL       PRIMARY KEY,
    story_id        VARCHAR(36)     NOT NULL,
    agent_version   VARCHAR(20)     NOT NULL,
    genome_data     JSONB           NOT NULL,
    overall_confidence  DECIMAL(3,2),
    relationship_type   VARCHAR(50),
    outcome_type        VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_genomes_story_id ON relationship_genomes(story_id);
CREATE INDEX idx_genomes_relationship_type ON relationship_genomes(relationship_type);
CREATE INDEX idx_genomes_overall_confidence ON relationship_genomes(overall_confidence);
CREATE INDEX idx_genomes_created_at ON relationship_genomes(created_at);
```

[Source: _bmad-output/architecture.md#PostgreSQL — relationship_genomes 表]

#### `chat_memories` 表

```sql
CREATE TABLE chat_memories (
    id                  BIGSERIAL       PRIMARY KEY,
    conversation_id     VARCHAR(255)    NOT NULL,
    content             TEXT            NOT NULL,
    type                VARCHAR(20)     NOT NULL,  -- USER / ASSISTANT / SYSTEM
    "timestamp"         TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_memories_conversation ON chat_memories(conversation_id);
```

[Source: _bmad-output/architecture.md#PostgreSQL — chat_memories 表（Spring AI 框架管理）]

#### `api_keys` 表

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

[Source: _bmad-output/architecture.md#PostgreSQL — api_keys 表]

### 启动类变更要点

**当前状态（Story 1.1）：**
```java
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        MongoAutoConfiguration.class,
        DataMongoAutoConfiguration.class,
        DataMongoRepositoriesAutoConfiguration.class
})
```

**目标状态（Story 1.2）：**
```java
@SpringBootApplication
```

- 删除 `exclude` 属性
- 删除 4 个 AutoConfiguration 类的 import 语句
- 保留 `@MapperScan("com.rkos.modules.*.mapper")` 不变

**⚠️ Spring Boot 4.1 包路径提醒：**
- `DataSourceAutoConfiguration` → `org.springframework.boot.jdbc.autoconfigure`
- `MongoAutoConfiguration` → `org.springframework.boot.mongodb.autoconfigure`
- `DataMongoAutoConfiguration` → `org.springframework.boot.data.mongodb.autoconfigure`
- `DataMongoRepositoriesAutoConfiguration` → `org.springframework.boot.data.mongodb.autoconfigure`

移除 exclude 后这些 import 语句必须一并删除，否则编译报错。

### 连接池配置建议

**开发环境 `application-dev.yml`（HikariCP 默认值通常足够）：**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
```

**生产环境 `application-prod.yml`：**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 前一个 Story 情报（Story 1.1）

| 项目 | 内容 |
|------|------|
| 文件创建 | `pom.xml`、`RkosApplication.java`、`application.yml`、`application-dev.yml`、`application-prod.yml` 等 |
| 代码模式 | Spring Boot 4.1.0 + Java 21，包名 `com.rkos`，Lombok |
| 已知问题 | Spring Boot 4.1 自动配置类包路径重组（已记录正确路径） |
| 编译命令 | `mvn clean compile -Dmaven.test.skip=true` |
| Deferred 项 | `@MapperScan` 与 DataSource 排除并存（本 Story 解决）、Flyway 版本评估（本 Story 评估） |

### 本地数据库启动方式（无 Docker Compose）

Story 1.9 才创建 `docker-compose.yml`，本 Story 需手动启动数据库：

```bash
# MongoDB
docker run -d --name rkos-mongo -p 27017:27017 mongo:7

# PostgreSQL
docker run -d --name rkos-postgres -p 5432:5432 \
  -e POSTGRES_DB=rkos_dev \
  -e POSTGRES_USER=dev_user \
  -e POSTGRES_PASSWORD=dev_password \
  postgres:16
```

### 验证检查清单

- [x] `mvn clean compile` 编译成功
- [x] MongoDB 和 PostgreSQL Docker 容器运行中
- [x] `mvn spring-boot:run` 启动无报错（1.837s）
- [x] 日志包含 `Successfully applied 1 migration to schema "public"`
- [x] 日志包含 MongoDB 连接成功信息（state=CONNECTED）
- [x] `psql -h localhost -U dev_user -d rkos_dev -c "\dt"` 显示 3 张表 + flyway_schema_history
- [x] 索引已创建：`\di` 确认 11 个索引存在（含 6 个业务索引 + 5 个系统索引）

### Project Structure Notes

本 Story 涉及的文件变更：

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/rkos/RkosApplication.java` | 修改 | 移除 exclude 和相关 import |
| `backend/src/main/resources/db/migration/V1__init_schema.sql` | 新建 | Flyway 迁移脚本 |
| `backend/src/main/resources/application-dev.yml` | 修改 | 添加 HikariCP 配置（可选） |
| `backend/src/main/resources/application-prod.yml` | 修改 | 添加 HikariCP 配置（可选） |
| `backend/src/main/java/com/rkos/config/MongoConfig.java` | 可选新建 | 仅在需要自定义 MongoDB 配置时 |
| `backend/src/main/java/com/rkos/config/PostgresConfig.java` | 可选新建 | 仅在需要自定义 PostgreSQL 配置时 |

### References

- [Source: _bmad-output/epics.md#Story 1.2：双数据库配置与迁移脚本] — 验收标准定义
- [Source: _bmad-output/architecture.md#PostgreSQL — relationship_genomes 表] — 表结构和索引
- [Source: _bmad-output/architecture.md#PostgreSQL — chat_memories 表] — 表结构和索引
- [Source: _bmad-output/architecture.md#PostgreSQL — api_keys 表] — 表结构和索引
- [Source: _bmad-output/architecture.md#决策7：双环境配置策略] — dev/prod 配置差异
- [Source: _bmad-output/architecture.md#决策1：四存储演进策略] — 双存储架构决策
- [Source: _bmad-output/1-1-project-skeleton-init.md#启动类数据库排除策略] — 当前排除配置
- [Source: _bmad-output/1-1-project-skeleton-init.md#Review Findings] — 代码审查 deferred 项
- [Source: _bmad-output/deferred-work.md] — 延迟工作记录

## Dev Agent Record

### Agent Model Used

Qoder AI（当前会话）

### Debug Log References

- `mvn clean compile` 编译成功（BUILD SUCCESS，0.989s）
- **Spring Boot 4.x Flyway 模块化问题发现并修复**：原始 `flyway-core` + `flyway-database-postgresql` 在 Spring Boot 4.1 下不触发自动配置，需改用 `spring-boot-starter-flyway` + `flyway-database-postgresql`
- 运行时验证全部通过：
  - HikariPool-1 启动成功
  - Flyway: "Successfully applied 1 migration to schema 'public', now at version v1"
  - MongoDB: Monitor thread connected to localhost:27017, state=CONNECTED
  - 应用启动: Started RkosApplication in 1.837 seconds
  - psql 确认 3 张表 + flyway_schema_history + 11 个索引全部创建成功
- 手动配置类评估：Spring Boot 4.1 自动配置已覆盖 MongoDB + PostgreSQL + HikariCP，无需额外配置类

### Completion Notes List

- ✅ Task 1：移除 `RkosApplication.java` 中 4 个 exclude 类和对应 import，更新 JavaDoc 注释
- ✅ Task 2：创建 `V1__init_schema.sql`，包含 3 张表 + 6 个索引，完全按 architecture.md DDL
- ✅ Task 3：dev 环境 HikariCP（pool=10, idle=2），prod 环境（pool=20, idle=5, idle-timeout=600s, max-lifetime=1800s）
- ✅ Task 4：**运行时验证全部通过** — Docker 容器启动 + spring-boot:run + Flyway 迁移执行 + MongoDB 连接 + psql 确认表和索引
- ✅ Task 5：评估结论 — 无需手动配置类
- ⚠️ 修复：pom.xml 将 `flyway-core`+`flyway-database-postgresql`（显式版本）替换为 `spring-boot-starter-flyway`+`flyway-database-postgresql`（BOM 管理），删除 `flyway.version` 属性

### File List

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/pom.xml` | 修改 | Flyway 依赖改为 `spring-boot-starter-flyway` + `flyway-database-postgresql`，删除 `flyway.version` 属性 |
| `backend/src/main/java/com/rkos/RkosApplication.java` | 修改 | 移除 exclude 和 4 个 import，更新 JavaDoc |
| `backend/src/main/resources/db/migration/V1__init_schema.sql` | 修改 | Flyway 迁移脚本（3 表 + 6 索引），`chat_memories.timestamp` 已重命名为 `created_at` |
| `backend/src/main/resources/application-dev.yml` | 修改 | 添加 HikariCP 连接池配置 |
| `backend/src/main/resources/application-prod.yml` | 修改 | 添加 HikariCP 生产级连接池配置 |

### Change Log

- 2025-07-16：Story 1.2 实施完成 — 双数据库配置启用、Flyway 迁移脚本创建、HikariCP 连接池配置、运行时全量验证通过
- 2025-07-16：修复 Flyway 依赖（Spring Boot 4.x 模块化：需 `spring-boot-starter-flyway` 而非原始 `flyway-core`）
- 2025-07-16：代码审查修复 PATCH-1 — `chat_memories` 表 `"timestamp"` 重命名为 `created_at`（解决 Spring AI JdbcChatMemoryRepository 兼容性问题）

### Deferred 项（传递给后续 Story）

| 编号 | 内容 | 目标 Story | 说明 |
|------|------|------------|------|
| DEFER-1 | `relationship_genomes.updated_at` 缺少自动更新触发器 | `2-3-genome-model-postgres-storage` | 创建 Mapper/Service 层时补充 `BEFORE UPDATE` 触发器或在应用层显式设置 `updated_at` |
| DEFER-2 | `api_keys.is_active` 缺少索引 | `1-7-api-key-authentication` | 实现 API Key 认证逻辑时根据实际查询模式评估补充 `CREATE INDEX idx_api_keys_is_active ON api_keys(is_active)` |
