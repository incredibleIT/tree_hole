-- =============================================================================
-- RKOS 数据库初始化迁移脚本
-- Flyway V1 — 创建 PostgreSQL 结构化存储表
-- 涉及表：relationship_genomes, chat_memories, api_keys
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. relationship_genomes（关系基因组表）
--    存储 Agent 从原始故事中提取的结构化关系知识
-- ---------------------------------------------------------------------------
CREATE TABLE relationship_genomes (
    id                  BIGSERIAL       PRIMARY KEY,
    story_id            VARCHAR(36)     NOT NULL,
    agent_version       VARCHAR(20)     NOT NULL,
    genome_data         JSONB           NOT NULL,
    overall_confidence  DECIMAL(3,2),
    relationship_type   VARCHAR(50),
    outcome_type        VARCHAR(50),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_genomes_story_id
    ON relationship_genomes(story_id);

CREATE INDEX idx_genomes_relationship_type
    ON relationship_genomes(relationship_type);

CREATE INDEX idx_genomes_overall_confidence
    ON relationship_genomes(overall_confidence);

CREATE INDEX idx_genomes_created_at
    ON relationship_genomes(created_at);

-- ---------------------------------------------------------------------------
-- 2. chat_memories（对话记忆表）
--    存储用户与 Agent 的多轮对话记录（Spring AI 框架管理）
-- ---------------------------------------------------------------------------
CREATE TABLE chat_memories (
    id                  BIGSERIAL       PRIMARY KEY,
    conversation_id     VARCHAR(255)    NOT NULL,
    content             TEXT            NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_memories_conversation
    ON chat_memories(conversation_id);

-- ---------------------------------------------------------------------------
-- 3. api_keys（API 密钥表）
--    管理外部调用方的 API Key 认证
-- ---------------------------------------------------------------------------
CREATE TABLE api_keys (
    id              BIGSERIAL       PRIMARY KEY,
    key_hash        VARCHAR(64)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP
);

CREATE UNIQUE INDEX idx_api_keys_key_hash
    ON api_keys(key_hash);
