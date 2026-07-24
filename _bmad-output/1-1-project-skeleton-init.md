# Story 1.1：项目骨架初始化

Status: done

## Story

作为**开发者**，
我希望有一个可运行的 Spring Boot 项目骨架，
以便在此基础上进行功能开发。

## Acceptance Criteria

1. **Given** 项目根目录 `backend/` 存在 `pom.xml`
   **When** 执行 `mvn clean compile`
   **Then** 编译成功，无错误
   **And** `RkosApplication.java` 可正常启动（Spring Boot 4.1.0 + Java 21）
   **And** `application.yml` 包含 dev/prod profile 基础配置

## Tasks / Subtasks

- [x] Task 1：通过 Spring Initializr 生成项目骨架（AC: #1）
  - [x] Subtask 1.1：创建 `backend/` 目录
  - [x] Subtask 1.2：手动创建 Maven 项目（Spring Boot 4.1.0、Java 21、包名 `com.rkos`）
  - [x] Subtask 1.3：引入初始依赖：web、validation、lombok、actuator
  - [x] Subtask 1.4：验证 `pom.xml` 生成正确，`mvn clean compile` 编译通过
- [x] Task 2：配置 pom.xml 完整依赖（AC: #1）
  - [x] Subtask 2.1：添加 Spring AI BOM（2.0.0）到 `<dependencyManagement>`
  - [x] Subtask 2.2：添加 MongoDB starter（`spring-boot-starter-data-mongodb`）
  - [x] Subtask 2.3：添加 PostgreSQL + MyBatis-Plus（`mybatis-plus-spring-boot4-starter`、`mybatis-plus-jsqlparser`、`spring-boot-starter-jdbc`）
  - [x] Subtask 2.4：添加 Flyway 依赖（`flyway-core`、`flyway-database-postgresql`）
  - [x] Subtask 2.5：添加 springdoc-openapi（Swagger UI）依赖
  - [x] Subtask 2.6：添加 Spring AI OpenAI starter（暂不激活，后续 Story 使用）
  - [x] Subtask 2.7：验证 `mvn clean compile` 编译通过
- [x] Task 3：创建启动类 RkosApplication.java（AC: #1）
  - [x] Subtask 3.1：在 `src/main/java/com/rkos/RkosApplication.java` 创建主启动类
  - [x] Subtask 3.2：验证 Spring Boot 应用可正常启动（排除自动配置以允许无 DB 启动）
- [x] Task 4：配置 application.yml 多环境（AC: #1）
  - [x] Subtask 4.1：创建 `application.yml`（公共配置：server.port=8080、spring.profiles.active=dev）
  - [x] Subtask 4.2：创建 `application-dev.yml`（开发环境：数据库 localhost、日志 DEBUG、Actuator 全暴露）
  - [x] Subtask 4.3：创建 `application-prod.yml`（生产环境：数据库服务名连接、日志 INFO/WARN、Actuator 仅 health/info）
  - [x] Subtask 4.4：配置 `spring.data.mongodb` 和 `spring.datasource`（PostgreSQL）占位，数据库暂不可用不影响启动
- [x] Task 5：创建基础项目目录结构（AC: #1）
  - [x] Subtask 5.1：创建 `src/main/java/com/rkos/config/` 包（package-info.java 占位）
  - [x] Subtask 5.2：创建 `src/main/java/com/rkos/common/` 包（package-info.java 占位）
  - [x] Subtask 5.3：创建 `src/main/java/com/rkos/modules/story/` 子包结构（controller/service/agent/repository/model/dto）
  - [x] Subtask 5.4：创建 `src/main/resources/prompts/` 目录（.gitkeep）
  - [x] Subtask 5.5：创建 `src/main/resources/db/migration/` 目录（.gitkeep）
  - [x] Subtask 5.6：创建 `src/test/java/com/rkos/` 测试目录
- [x] Task 6：验证项目可编译运行（AC: #1）
  - [x] Subtask 6.1：执行 `mvn clean compile` 确认编译成功
  - [x] Subtask 6.2：执行 `mvn spring-boot:run` 确认应用启动（排除数据库自动配置以允许无 DB 启动）
  - [x] Subtask 6.3：确认 Actuator `/actuator/health` 端点可访问

### Review Findings

- [x] [Review][Patch] 启动类排除策略代码示例需更新为 4 个类（增加 DataMongoRepositoriesAutoConfiguration），且类名使用 Spring Boot 4.1 正确包路径 [`RkosApplication.java`:17-22] ✅ 已修复
- [x] [Review][Patch] 项目结构约定需补充 mapper/ 子目录（story/knowledge/user 各模块） [1-1-project-skeleton-init.md:80-101] ✅ 已修复
- [x] [Review][Patch] 项目结构约定需补充 `src/main/resources/mapper/` XML 目录 [1-1-project-skeleton-init.md:94-101] ✅ 已修复
- [x] [Review][Patch] 启动类示例需补充 `@MapperScan("com.rkos.modules.*.mapper")` 注解 [1-1-project-skeleton-init.md:158-168] ✅ 已修复
- [x] [Review][Patch] `type-aliases-package` 配置指向 `entity` 包但项目实际使用 `model` 包，已统一为 `model` [`application-dev.yml`:27, `application-prod.yml`:27] ✅ 已修复
- [x] [Review][Defer] `@MapperScan` 与 DataSource 排除并存——当前阶段 Mapper 扫描为空操作，Story 1.2 配置数据库后自然解决 — deferred
- [x] [Review][Defer] Spring AI OpenAI Starter 自动配置未排除——占位 API Key 不影响启动，Story 2.1 接入 LLM 时处理 — deferred
- [x] [Review][Defer] Flyway 版本手动指定覆盖 Spring Boot BOM 管理版本——当前验证通过，Story 1.2 数据库迁移时评估 — deferred
- [x] [Review][Defer] 生产环境 MongoDB 连接未配置认证——Docker Compose 在 Story 1.9，认证配置随之添加 — deferred
- [x] [Review][Defer] 生产环境必需环境变量无默认值，缺少时启动报错不够优雅——部署阶段（Story 4.4）处理 — deferred
- [x] [Review][Defer] Jackson 2.x/3.x 双版本共存风险——当前启动验证通过，后续集成 LLM 时监控 — deferred

## Dev Notes

### 技术栈版本锁定

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | Spring Boot 4.1 基线，虚拟线程支持 |
| Spring Boot | 4.1.0 | 2026-06-10 GA，Jakarta EE 11，Spring Framework 7.0 |
| Spring AI | 2.0.0 GA | 2026-06-12 GA，要求 Spring Boot 4.1+，Jackson 3 |
| Maven | 3.6.3+ | 构建工具 |
| Flyway | 12.4.0 | 数据库迁移（Spring Boot 4.1 内置管理） |
| MongoDB Driver | 5.8.0 | Spring Boot 4.1 管理版本 |
| Jackson | 3.x | Spring AI 2.0 要求，包名为 `tools.jackson` |

### 关键技术注意事项

1. **Spring Boot 4.1 虚拟线程默认开启**：Java 21+ 环境下 Tomcat 工作线程池自动使用虚拟线程，无需额外配置
2. **Jackson 3 包名变更**：从 `com.fasterxml.jackson` 迁移到 `tools.jackson`，注意 import 语句
3. **Maven 跳过测试**：Spring Boot 4.1 中 `-DskipTests` 不再跳过 AOT 测试处理，需用 `-Dmaven.test.skip=true`
4. **Spring AI BOM 引入方式**：在 `<dependencyManagement>` 中 import `spring-ai-bom` 2.0.0，所有 Spring AI 子依赖版本由 BOM 统一管理
5. **Spring AI DashScope 暂不兼容 2.0**：Spring AI Alibaba 仍基于 1.x，当前使用 Spring AI 原生 OpenAI starter（兼容通义千问的 OpenAI 兼容接口）

### 项目结构约定

项目位于 `backend/` 目录下（与前端 `frontend/` 平级）。

```
backend/
├── pom.xml
├── src/main/java/com/rkos/
│   ├── RkosApplication.java
│   ├── config/
│   ├── common/
│   └── modules/
│       ├── story/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── agent/
│       │   ├── mapper/          # MyBatis-Plus PostgreSQL Mapper
│       │   ├── repository/      # Spring Data MongoDB Repository
│       │   ├── model/
│       │   └── dto/
│       ├── knowledge/
│       │   └── mapper/
│       └── user/
│           └── mapper/
├── src/main/resources/
│   ├── prompts/
│   ├── db/migration/
│   ├── mapper/                 # MyBatis Mapper XML 文件
│   │   ├── story/
│   │   ├── knowledge/
│   │   └── user/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-prod.yml
└── src/test/java/com/rkos/
```

### 命名规范

- Java 类名：`PascalCase`
- 方法/变量：`camelCase`
- 包名：`com.rkos.{module}.{submodule}`（全小写）
- 配置文件：`application-{profile}.yml`
- 数据库表名/字段名：`snake_case`（后续 Story 使用）

### application.yml 关键配置项

```yaml
# application.yml（公共配置）
server:
  port: 8080

spring:
  profiles:
    active: dev
  application:
    name: rkos-backend
```

```yaml
# application-dev.yml（开发环境）
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
        include: "*"

rkos:
  api:
    key: dev-api-key-12345
```

### 启动类数据库排除策略

由于 Story 1.1 阶段数据库尚未配置（Docker Compose 在 Story 1.9），启动类需要排除数据库自动配置：

```java
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,           // org.springframework.boot.jdbc.autoconfigure
        MongoAutoConfiguration.class,                // org.springframework.boot.mongodb.autoconfigure
        DataMongoAutoConfiguration.class,            // org.springframework.boot.data.mongodb.autoconfigure
        DataMongoRepositoriesAutoConfiguration.class // org.springframework.boot.data.mongodb.autoconfigure
})
@MapperScan("com.rkos.modules.*.mapper")
public class RkosApplication {
    public static void main(String[] args) {
        SpringApplication.run(RkosApplication.class, args);
    }
}
```

后续 Story 1.2 配置数据库后再移除 exclude。

### References

- [Source: _bmad-output/architecture.md#项目初始化命令] — Spring Initializr 命令和依赖配置
- [Source: _bmad-output/architecture.md#项目结构模式] — 完整目录树和命名规范
- [Source: _bmad-output/architecture.md#决策7：双环境配置策略] — dev/prod profile 配置差异
- [Source: _bmad-output/epics.md#Story 1.1] — 验收标准定义

### Project Structure Notes

- 项目与现有 `frontend/` 目录平级，放在 `backend/` 下
- 后端项目有独立的 `.gitignore`（Maven 标准）
- 遵循架构文档中定义的分层结构（config/common/modules）

## Dev Agent Record

### Agent Model Used

AI 助手（代码实现）

### Debug Log References

- Spring Boot 4.1.0 自动配置类包路径变更：`org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration` → `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`，`MongoAutoConfiguration` → `org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration`，`MongoDataAutoConfiguration` → `org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration`
- 未使用 Spring Initializr CLI，手动创建 pom.xml 和项目结构（等效结果）

### Completion Notes List

- ✅ 项目骨架完整创建，包含所有规划依赖（Spring Boot 4.1.0 + Spring AI 2.0.0 + Java 21）
- ✅ `mvn clean compile` 编译成功（1.069s）
- ✅ 应用启动成功（1.448s），无数据库依赖启动
- ✅ `/actuator/health` 返回 `{"status":"UP"}`
- ✅ `/v3/api-docs` 返回 200（springdoc 集成验证通过）
- ✅ 排除 4 个数据库自动配置类，Story 1.2 配置数据库后移除
- ⚠️ 发现 Spring Boot 4.1.0 自动配置类包路径重组，已记录正确路径供后续 Story 参考

### File List

- `backend/pom.xml`（新建）
- `backend/.gitignore`（新建）
- `backend/src/main/java/com/rkos/RkosApplication.java`（新建）
- `backend/src/main/java/com/rkos/config/package-info.java`（新建）
- `backend/src/main/java/com/rkos/common/package-info.java`（新建）
- `backend/src/main/java/com/rkos/modules/story/package-info.java`（新建）
- `backend/src/main/resources/application.yml`（新建）
- `backend/src/main/resources/application-dev.yml`（新建）
- `backend/src/main/resources/application-prod.yml`（新建）
- `backend/src/main/resources/prompts/.gitkeep`（新建）
- `backend/src/main/resources/db/migration/.gitkeep`（新建）
- `backend/src/test/java/com/rkos/.gitkeep`（新建）
- `backend/src/main/java/com/rkos/modules/story/controller/.gitkeep`（新建）
- `backend/src/main/java/com/rkos/modules/story/service/.gitkeep`（新建）
- `backend/src/main/java/com/rkos/modules/story/agent/.gitkeep`（新建）
- `backend/src/main/java/com/rkos/modules/story/repository/.gitkeep`（新建）
- `backend/src/main/java/com/rkos/modules/story/model/.gitkeep`（新建）
- `backend/src/main/java/com/rkos/modules/story/dto/.gitkeep`（新建）

### Change Log

- 2026-07-16：Story 1.1 完整实现，所有 6 个 Task（22 个 Subtask）均已完成。项目骨架通过编译和启动验证。
