-- =============================================================================
-- RKOS Flyway V2 — 种子开发环境 API Key
-- 插入一条开发用 API Key（明文: dev-api-key-12345）
-- key_hash = SHA-256("dev-api-key-12345")
-- =============================================================================

INSERT INTO api_keys (key_hash, name, is_active, created_at, expires_at)
VALUES (
    '8264dc9f07e749d9c2ffead0b25de8cb22bed7af774e189ef224ae015908776b',
    'Development Key',
    TRUE,
    NOW(),
    NULL
);
