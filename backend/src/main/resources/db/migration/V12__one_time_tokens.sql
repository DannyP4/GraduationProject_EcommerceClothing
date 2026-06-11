-- V12: single-use tokens (password reset, email verify, admin invite, oauth handoff)

SET NAMES utf8mb4;

CREATE TABLE one_time_tokens (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED DEFAULT NULL,
    email       VARCHAR(255) NOT NULL,
    token_hash  CHAR(64) NOT NULL,
    type        VARCHAR(32) NOT NULL,
    payload     JSON DEFAULT NULL,
    expires_at  TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL DEFAULT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ott_token_hash (token_hash),
    KEY idx_ott_email_type (email, type),
    CONSTRAINT fk_ott_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
