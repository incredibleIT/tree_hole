# 延迟工作记录

## Deferred from: code review of story-1.1 (2025-07-16)

- `@MapperScan` 与 DataSource 排除并存：当前阶段 Mapper 扫描为空操作，Story 1.2 配置数据库后自然解决
- Spring AI OpenAI Starter 自动配置未排除：占位 API Key 不影响启动，Story 2.1 接入 LLM 时处理
- Flyway 版本手动指定（12.4.0）覆盖 Spring Boot BOM 管理版本：当前验证通过，Story 1.2 数据库迁移时评估
- 生产环境 MongoDB 连接未配置认证：Docker Compose 在 Story 1.9，认证配置随之添加
- 生产环境必需环境变量无默认值，缺少时启动报错不够优雅：部署阶段（Story 4.4）处理
- Jackson 2.x/3.x 双版本共存风险：当前启动验证通过，后续集成 LLM 时监控

## Deferred from: code review of story-1.3 (2025-07-16)

- `ConstraintViolationException` 未专门处理：架构文档未定义此 handler，方法级 @Valid 校验场景待后续 Story 补充
- `MissingServletRequestPartException` 未专门处理：架构文档未定义此 handler，文件上传场景待后续 Story 补充

## Deferred from: code review of story-1.4 (2025-07-16)

- `contentLength` 自动计算机制：架构文档定义 save 时由 `content.length()` 计算，当前模型层无 `@PrePersist` 回调，延迟到 Story 1.5 Service 层实现

## Deferred from: code review of story-1.5 (2025-07-16)

- `StoryService.save()` 无 MongoDB 异常语义处理：MVP 阶段 GenericExceptionHandler 已兜底（500），后续可添加 `DataAccessException` 专用 handler 返回更友好的错误信息
- `GlobalExceptionHandler` Collectors.toMap 空值风险：预先存在（Story 1-3），`FieldError::getDefaultMessage` 理论上可返回 null，但当前所有校验注解均设 message 属性，实际不触发

## Deferred from: code review of story-1.6 (2026-07-19)

- 空字符串查询参数未过滤：`?relationshipType=` 传空字符串时作为有效过滤值，返回空结果而非全部，需全局 API 策略（如自定义 Converter 或 Service 层 blank→null 转换）
- 分页参数非法值返回 500：`page=-1` 或 `size=0` 时 `PageRequest.of()` 抛 `IllegalArgumentException`，`GlobalExceptionHandler` 无对应 handler，属全局改进项

## Deferred from: code review of story-2.1 (2026-07-20)

- `retry_count` 日志字段未实现：spec Dev Notes 要求记录 retry_count，但 `@Retryable` 方法内不易获取（需 `RetrySynchronizationManager`），后续完善

## Deferred from: code review of story-2.2 (2026-07-20)

- 模板文件无大小限制：`PromptTemplateService.loadTemplate()` 将模板内容一次性读入内存，超大模板文件可能导致 OOM。实际模板文件远小于内存（KB 级），后续评估是否需要增加大小上限

## Deferred from: code review of 2-3-genome-model-postgres-storage (2026-07-16)

- 集成测试未通过 MyBatis-Plus 管道：`@SpringBootTest` 因 Spring Boot 4.x 与 flapdoodle 兼容性不可用，直连 JDBC 方式是当前最佳替代，GenomeMapper end-to-end 验证留待后续 Story 补充
- `buildFullGenomeData` 辅助方法在 `GenomeModelTest` 和 `GenomeMapperTest` 中重复（~70 行），提取到共享测试工具类是优化项
- `selectByStoryId` 测试使用原生 JDBC 而非 GenomeMapper，与 Spring Boot 4.x 兼容性限制同源
- `JsonbTypeHandler` 自建 static ObjectMapper，与 `Jackson2Config` Spring 管理实例配置独立，MyBatis TypeHandler 不走 Spring DI 是架构约束
- 集成测试硬编码数据库凭证 `dev_user/dev_password`，提取到 test properties 或环境变量是优化项
- `updated_at` 字段无 UPDATE 触发器，UPDATE 操作不会自动更新该字段，全表共性问题

## Deferred from: code review of 2-5-genome-persistence-status-mgmt (2026-07-24)

- `MODEL_USED` 硬编码为 `dashscope/qwen-max`：`StoryPersistenceService` 中 Agent 版本和模型名称均为常量，后续配置化（如 `@Value` 或 `application.yml`）时统一处理

## Deferred from: code review of 2-6-async-trigger-on-submit (2026-07-16)

- `persistGenome` 崩溃可能留下孤儿 PROCESSING 状态：Story 2-5 范围，定时补偿扫描（Story 2-7 或后续）处理
- CallerRunsPolicy 降级同步时可能阻塞 HTTP 线程：已知设计折衷（50 队列 + 10 线程 = 60 并发才触发），运维调优阶段处理
- AC#5 无并发测试验证：纯 Mockito 无法测真实并发，已知限制，后续集成测试补充
- MongoDB save 同步阻塞：Story 1-5 设计决策，不在 Story 2-6 范围

## Deferred from: code review of story-2.4 (2026-07-22)

- `GenomeData.storyId` / `genomeId` 未填充：LLM 解析后 JSONB 内部这两个字段为 null，而顶层 `RelationshipGenome.storyId` 已设置，存在数据不一致。Story 2-3 数据模型设计遗留，可在 Story 2-5（持久化）或数据模型重构时处理

## Deferred from: code review of 2-7-story-reprocess (2026-07-16)

- `upsertByStoryId` delete+insert 无事务保护，并发重新处理时可能数据丢失：架构层限制（无分布式事务），并发场景极罕见，需架构决策
- `reprocessStory` 不预更新 MongoDB 为 REPROCESSING，409 检查存在竞态窗口：规格明确为有意设计（与 submitStory 模式一致），需分布式锁或状态机彻底解决
- `retryCount(0)` 每次状态更新硬编码重置：Story 2-5 已有模式，补偿机制在后续 Story
- `updateProcessingStatus` REPROCESSING 阶段 MongoDB 故障不像 PROCESSING 一样阻断 PostgreSQL 写入：与 Story 2-6 延迟项“MongoDB sync 阻塞”同源

## Deferred from: code review of 2-8-test-seed-data (2026-07-23)

- ObjectMapper 配置重复（SeedDataLoader vs GenomeMapperTest 各自维护独立实例，配置相同）：预存在问题（Story 2-3 已存在），可提取共享 TestObjectMapper 工具类
- seed storyId 非 UUID 格式（`seed-story-001` 等）：当前代码无 UUID 校验， Epic 3 查询 API 若添加 UUID 格式校验需注意

---

## Epic 2 回顾行动项（2026-07-24）

### A2：技术准备 Story（Epic 3 前集中处理）

- [ ] ObjectMapper 共享配置：抽取 TestObjectMapper 工具类，统一 SeedDataLoader / GenomeMapperTest / JsonbTypeHandler 的配置（deferred #3 + #8）
- [ ] persistGenome 崩溃孤儿 PROCESSING：增加 finally 块回滚或定时补偿扫描（deferred #7）
- [ ] upsertByStoryId 事务保护：改为 INSERT ON CONFLICT 或加 @Transactional（deferred #11）

### A3：顺手修复（并入 A2）

- [ ] retry_count 日志字段：使用 RetrySynchronizationManager 获取计数（deferred #1）
- [ ] MODEL_USED 硬编码：从 LLM 响应或配置提取（deferred #10）

### A4：逐步消化（Epic 3 期间按需）

- [ ] 模板文件大小限制（deferred #2）
- [ ] 硬编码数据库凭证外部化（deferred #5）
- [ ] updated_at 无 UPDATE 触发器（deferred #6）
- [ ] seed storyId 非 UUID 格式（deferred #9，Epic 3 需注意）
- [ ] CallerRunsPolicy 阻塞风险（deferred #12）
- [ ] 无并发测试（deferred #13）
