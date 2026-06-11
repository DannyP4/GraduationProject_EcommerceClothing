-- V13: OAuth sign-in — nullable password + provider identity
ALTER TABLE users
    MODIFY COLUMN password_hash VARCHAR(255) NULL,
    ADD COLUMN auth_provider VARCHAR(16) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN oauth_subject VARCHAR(255) NULL,
    ADD CONSTRAINT uq_users_oauth_subject UNIQUE (oauth_subject);
