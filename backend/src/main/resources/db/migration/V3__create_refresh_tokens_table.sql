-- V3__create_refresh_tokens_table.sql
-- Refresh tokens da aplicação.
-- Armazenamos apenas o SHA-256 hash — nunca o valor puro.

CREATE TABLE refresh_tokens (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,   -- SHA-256 hex (64 chars)
    expires_at  DATETIME(6) NOT NULL,
    revoked_at  DATETIME(6) NULL,       -- NULL = ativo
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT uq_rt_hash UNIQUE (token_hash),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,

    INDEX idx_rt_user_id (user_id),
    INDEX idx_rt_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
