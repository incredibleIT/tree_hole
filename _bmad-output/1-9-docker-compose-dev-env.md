# Story 1.9：Docker Compose 开发环境

Status: done

## Story

作为**开发者**，
我希望一键启动完整的开发环境（应用 + MongoDB + PostgreSQL），
以便本地开发和调试。

## Acceptance Criteria

1. **Given** 项目 `backend/` 目录存在 `Dockerfile` 和 `docker-compose.yml`
   **When** 在 `backend/` 目录执行 `docker compose up`
   **Then** Spring Boot 应用、MongoDB、PostgreSQL 三个容器正常启动
   **And** 应用容器成功连接到 MongoDB 和 PostgreSQL
   **And** 应用容器使用 `dev` Profile 启动

2. **Given** `Dockerfile` 使用 Java 21 基础镜像
   **When** 执行 `docker build -t rkos-backend .`
   **Then** 构建成功，生成可运行镜像
   **And** 镜像使用多阶段构建（Maven 构建阶段 + JRE 运行阶段）

3. **Given** `.env.example` 包含所有必要的环境变量说明
   **When** 开发者参照 `.env.example` 创建 `.env` 文件
   **Then** 所有环境变量有中文注释说明用途和默认值
   **And** `docker compose up` 可直接使用默认值启动

4. **Given** 应用容器启动完成
   **When** 访问 `http://localhost:8080/actuator/health`
   **Then** 返回 HTTP 200，表明应用健康
   **And** Flyway 迁移脚本已自动执行（V1 + V2）

## Tasks / Subtasks

- [x] Task 1：创建 `Dockerfile` 多阶段构建（AC: #2）
  - [x] Subtask 1.1：在 `backend/` 目录创建 `Dockerfile`
  - [x] Subtask 1.2：构建阶段使用 `maven:3.9-eclipse-temurin-21` 镜像执行 `mvn package -DskipTests`
  - [x] Subtask 1.3：运行阶段使用 `eclipse-temurin:21-jre` 轻量镜像
  - [x] Subtask 1.4：暴露 8080 端口，使用 `ENTRYPOINT` 启动 jar
- [x] Task 2：创建 `docker-compose.yml` 开发环境编排（AC: #1, #4）
  - [x] Subtask 2.1：定义 `rkos-backend` 服务（build 指向当前目录，depends_on MongoDB + PostgreSQL）
  - [x] Subtask 2.2：定义 `rkos-mongodb` 服务（`mongo:7` 镜像，端口 27017，数据卷持久化）
  - [x] Subtask 2.3：定义 `rkos-postgresql` 服务（`postgres:16-alpine` 镜像，端口 5432，数据卷持久化）
  - [x] Subtask 2.4：为应用服务设置 `SPRING_PROFILES_ACTIVE=dev` 和数据库连接环境变量
  - [x] Subtask 2.5：为应用服务配置 healthcheck（`curl /actuator/health`）
  - [x] Subtask 2.6：定义 `docker network` 和 `volumes`
- [x] Task 3：创建 `.env.example` 环境变量说明文件（AC: #3）
  - [x] Subtask 3.1：列出 MongoDB 相关变量（MONGO_INITDB_DATABASE 等）
  - [x] Subtask 3.2：列出 PostgreSQL 相关变量（POSTGRES_DB、POSTGRES_USER、POSTGRES_PASSWORD）
  - [x] Subtask 3.3：列出应用相关变量（SERVER_PORT、SPRING_PROFILES_ACTIVE、RKOS_API_KEY）
  - [x] Subtask 3.4：每个变量附中文注释说明用途和默认值
- [x] Task 4：调整 `application-dev.yml` 适配容器化环境（AC: #1）
  - [x] Subtask 4.1：将 MongoDB `host: localhost` 改为 `${MONGO_HOST:localhost}`（支持环境变量覆盖）
  - [x] Subtask 4.2：将 PostgreSQL `url` 中的 `localhost` 改为 `${POSTGRES_HOST:localhost}`
  - [x] Subtask 4.3：将数据库用户名/密码改为环境变量引用
- [x] Task 5：更新 `.gitignore`（AC: #1, #3）
  - [x] Subtask 5.1：在 `backend/.gitignore` 添加 `.env`（排除实际环境文件，保留 `.env.example`）

## Dev Notes

### 当前项目无 Docker 文件（全部新建）

项目中**不存在**任何 Docker 相关文件，本 Story 需要从零创建：

| 文件 | 操作 | 位置 |
|------|------|------|
| `backend/Dockerfile` | 新建 | 多阶段构建 |
| `backend/docker-compose.yml` | 新建 | 开发环境三容器 |
| `backend/.env.example` | 新建 | 环境变量说明 |
| `backend/src/main/resources/application-dev.yml` | 修改 | 环境变量引用 |
| `backend/.gitignore` | 修改 | 添加 `.env` |

[Source: 项目文件搜索确认无 Docker 文件]

### 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | `eclipse-temurin:21-jre` 运行镜像 |
| Maven | 3.9 | `maven:3.9-eclipse-temurin-21` 构建镜像 |
| MongoDB | 7 | `mongo:7` 官方镜像 |
| PostgreSQL | 16 | `postgres:16-alpine` 轻量镜像 |
| Spring Boot | 4.1.0 | 已有 |
| Flyway | 已有 | V1 + V2 迁移脚本自动执行 |

### 无 Maven Wrapper

项目**没有** `mvnw`/`mvnw.cmd`，Dockerfile 构建阶段必须使用 Maven 官方镜像：

```dockerfile
# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml 参考结构

```yaml
services:
  rkos-backend:
    build: .
    ports:
      - "${SERVER_PORT:-8080}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATA_MONGODB_HOST: rkos-mongodb
      SPRING_DATASOURCE_URL: jdbc:postgresql://rkos-postgresql:5432/${POSTGRES_DB:-rkos_dev}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-dev_user}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-dev_password}
      RKOS_API_KEY: ${RKOS_API_KEY:-dev-api-key-12345}
    depends_on:
      rkos-mongodb:
        condition: service_healthy
      rkos-postgresql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  rkos-mongodb:
    image: mongo:7
    ports:
      - "${MONGO_PORT:-27017}:27017"
    environment:
      MONGO_INITDB_DATABASE: ${MONGO_DB:-rkos_dev}
    volumes:
      - mongo-data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 5

  rkos-postgresql:
    image: postgres:16-alpine
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-rkos_dev}
      POSTGRES_USER: ${POSTGRES_USER:-dev_user}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-dev_password}
    volumes:
      - pg-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-dev_user} -d ${POSTGRES_DB:-rkos_dev}"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mongo-data:
  pg-data:
```

**注意**：应用容器内数据库连接地址使用**服务名**（`rkos-mongodb`、`rkos-postgresql`），而非 `localhost`。通过 `SPRING_DATA_MONGODB_HOST` 和 `SPRING_DATASOURCE_URL` 环境变量覆盖 `application-dev.yml` 中的 `localhost` 默认值。

### application-dev.yml 修改策略

当前 `application-dev.yml` 的数据库连接硬编码为 `localhost`，需要改为支持环境变量覆盖：

```yaml
# 修改前
spring:
  data:
    mongodb:
      host: localhost
  datasource:
    url: jdbc:postgresql://localhost:5432/rkos_dev
    username: dev_user
    password: dev_password

# 修改后（支持环境变量覆盖，本地开发仍可用 localhost）
spring:
  data:
    mongodb:
      host: ${MONGO_HOST:localhost}
      port: ${MONGO_PORT:27017}
      database: ${MONGO_DB:rkos_dev}
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:rkos_dev}
    username: ${POSTGRES_USER:dev_user}
    password: ${POSTGRES_PASSWORD:dev_password}
```

**关键**：使用 `${VAR:default}` 语法，确保本地无 Docker 开发（`mvn spring-boot:run`）仍然正常工作。

### 本 Story 不涉及的范围

- `docker-compose.prod.yml`（Story 4-4）
- `docker-compose.config.yml` Config Server（Story 4-3）
- 生产环境部署配置（Story 4-4）
- 部署文档（Story 4-5）
- 端到端集成测试（Story 4-6）

### 前一个 Story 情报（Story 1.8）

| 项目 | 内容 |
|------|------|
| 完成内容 | SwaggerConfig + springdoc 属性 + Controller/DTO OpenAPI 注解 + 单元测试 |
| 已有测试总数 | 57 个（全部通过） |
| 新增文件 | SwaggerConfig.java、SwaggerConfigTest.java |
| 测试策略 | 纯单元测试（不加载 Spring 上下文） |
| Java 环境 | `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` |
| 构建工具 | Maven（`mvn test`） |
| 代码审查延迟项 | Swagger 生产隔离（@Profile）— 与本 Story 无关 |

### 测试策略

本 Story 为**基础设施配置**，不涉及 Java 代码变更，无需新增单元测试。验证方式：

1. `docker compose config` — 验证 compose 文件语法正确
2. `docker build -t rkos-backend .` — 验证 Dockerfile 构建成功
3. `docker compose up` — 验证三容器启动并互联（手动验证）

现有 57 个 Java 单元测试必须继续通过：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn test`

### Project Structure Notes

本 Story 新增/修改的文件：

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/Dockerfile` | 新建 | 多阶段构建（Maven 构建 + JRE 运行） |
| `backend/docker-compose.yml` | 新建 | 开发环境三容器编排 |
| `backend/.env.example` | 新建 | 环境变量说明文件 |
| `backend/src/main/resources/application-dev.yml` | 修改 | 数据库连接改为环境变量引用 |
| `backend/.gitignore` | 修改 | 添加 `.env` 排除规则 |

不修改的文件：

| 文件 | 说明 |
|------|------|
| `pom.xml` | 不修改 |
| `application.yml` | 不修改 |
| `RkosApplication.java` | 不修改 |
| 所有 Controller/Service/DTO | 不修改 |
| 所有测试文件 | 不修改 |

### References

- [Source: _bmad-output/epics.md#Story 1.9：Docker Compose 开发环境 L257-270] — 验收标准定义
- [Source: _bmad-output/architecture.md#决策 8：Docker Compose 多环境支持 L608-658] — Docker Compose 策略
- [Source: _bmad-output/architecture.md#项目结构模式 L1241-1250] — Docker 文件位置定义
- [Source: backend/pom.xml L7-12] — Spring Boot 4.1.0 + Java 21
- [Source: backend/src/main/resources/application-dev.yml] — 当前数据库连接配置
- [Source: backend/src/main/resources/db/migration/V1__init_schema.sql] — Flyway 初始化脚本
- [Source: backend/src/main/resources/db/migration/V2__seed_dev_api_key.sql] — 开发 API Key 种子
- [Source: _bmad-output/1-8-api-versioning-swagger-docs.md] — 前一个 Story 完成情况

## Dev Agent Record

### Agent Model Used

Qwen (via Qoder IDE)

### Debug Log References

无调试问题。所有文件按规格文件要求创建/修改，`docker compose config` 验证通过，57 个单元测试全部通过。

### Code Review Findings

**审查层状态：** Blind Hunter ✓ | Edge Case Hunter ✓ | Acceptance Auditor ✓

| 分类 | 数量 | 详情 |
|------|------|------|
| patch（已修复） | 2 | #1 Dockerfile 添加非 root 用户 + chown /app、#5 .env.example 补充 MONGO_HOST/POSTGRES_HOST 变量说明 |
| defer | 1 | #4 MONGO_INITDB_DATABASE 仅在首次初始化时生效（MongoDB 官方镜像已知行为） |
| dismiss | 2 | #2 *.jar glob 安全（仅产生一个 jar）、#3 SPRING_DATA_MONGODB_PORT 硬编码合理（内部端口固定 27017） |

### Completion Notes List

- **Dockerfile**：多阶段构建，构建阶段 `maven:3.9-eclipse-temurin-21`，运行阶段 `eclipse-temurin:21-jre`；额外安装 `curl` 用于 healthcheck；新建 `.dockerignore` 排除不必要构建上下文文件
- **docker-compose.yml**：三容器编排（rkos-backend + rkos-mongodb + rkos-postgresql），数据库容器均配置 healthcheck，应用容器 `depends_on` 使用 `condition: service_healthy`；定义 `rkos-network` bridge 网络和 `mongo-data`/`pg-data` 数据卷
- **.env.example**：10 个环境变量，全部附中文注释说明用途和默认值
- **application-dev.yml**：MongoDB/PostgreSQL 连接和 API Key 全部改为 `${VAR:default}` 环境变量引用，本地 `mvn spring-boot:run` 仍可使用 localhost 默认值正常工作
- **backend/.gitignore**：添加 `.env` 排除规则（保留 `.env.example`）
- **验证结果**：`docker compose config` 通过；`mvn test` 57 个测试全部通过（0 失败、0 错误、0 跳过）

### Change Log

- 2026-07-20：完成 Story 1-9 全部 5 个 Task（Dockerfile + docker-compose.yml + .env.example + application-dev.yml 适配 + .gitignore 更新）

### File List

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/Dockerfile` | 新建 | 多阶段构建（Maven 构建 + JRE 运行） |
| `backend/.dockerignore` | 新建 | Docker 构建上下文排除文件 |
| `backend/docker-compose.yml` | 新建 | 开发环境三容器编排（Spring Boot + MongoDB 7 + PostgreSQL 16） |
| `backend/.env.example` | 新建 | 环境变量说明文件（10 个变量，中文注释） |
| `backend/src/main/resources/application-dev.yml` | 修改 | 数据库连接和 API Key 改为 `${VAR:default}` 环境变量引用 |
| `backend/.gitignore` | 修改 | 添加 `.env` 排除规则 |
