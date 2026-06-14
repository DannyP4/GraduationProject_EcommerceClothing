CREATE TABLE try_on_jobs (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id               BIGINT UNSIGNED NOT NULL,
    product_id            BIGINT UNSIGNED NOT NULL,
    status                VARCHAR(16)  NOT NULL,
    provider              VARCHAR(32)  NOT NULL,
    provider_request_id   VARCHAR(128) DEFAULT NULL,
    provider_response_url VARCHAR(500) DEFAULT NULL,
    user_image_url        VARCHAR(500) NOT NULL,
    garment_image_url     VARCHAR(500) NOT NULL,
    garment_photo_type    VARCHAR(16)  NOT NULL,
    category              VARCHAR(16)  DEFAULT NULL,
    result_image_url      VARCHAR(500) DEFAULT NULL,
    result_public_id      VARCHAR(255) DEFAULT NULL,
    error_message         VARCHAR(500) DEFAULT NULL,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_try_on_user_created (user_id, created_at),
    KEY idx_try_on_cache (user_id, product_id, status),
    CONSTRAINT fk_try_on_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_try_on_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
