-- V1__create_users_table.sql
-- Tabela principal de usuários.
-- email sem UNIQUE: usuários só-OAuth sem email (ex: Facebook sem permissão)
-- teriam múltiplos NULLs que violam unique no MySQL.

CREATE TABLE users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    email       VARCHAR(255)    NULL,
    password    VARCHAR(255)    NULL,       -- NULL para usuários só-OAuth
    name        VARCHAR(255)    NOT NULL,
    photo_url   VARCHAR(500)    NULL,
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
