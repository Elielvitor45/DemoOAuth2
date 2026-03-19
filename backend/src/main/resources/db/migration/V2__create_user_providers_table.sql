-- V2__create_user_providers_table.sql
-- Vincula um usuário a um ou mais provedores OAuth.
-- provider usa VARCHAR(20) com check constraint: evita o bug do Hibernate 6
-- que rejeita VARCHAR quando a entidade usa @Enumerated(EnumType.STRING).
-- Solução: anotar o campo com @Column(columnDefinition = "VARCHAR(20)")
-- no lado Java para que o Hibernate não tente criar/validar como ENUM.

CREATE TABLE user_providers (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL,   -- GOOGLE, FACEBOOK, LOCAL
    provider_id      VARCHAR(255) NULL,       -- ID no provedor; NULL para LOCAL
    access_token     TEXT         NULL,       -- Token do provedor OAuth
    token_expires_at DATETIME(6)  NULL,
    linked_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_used_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- Mesmo usuário externo não pode ser vinculado duas vezes
    CONSTRAINT uq_provider_provider_id UNIQUE (provider, provider_id),

    CONSTRAINT fk_up_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,

    INDEX idx_up_user_id (user_id),
    INDEX idx_up_provider (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
