-- V10: Product embeddings (AI retrieval core)
CREATE TABLE product_embeddings (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id   BIGINT UNSIGNED NOT NULL,
    model        VARCHAR(64)  NOT NULL,
    dim          INT          NOT NULL,
    content_hash CHAR(64)     NOT NULL,
    embedding    LONGBLOB     NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_embeddings_product (product_id),
    CONSTRAINT fk_product_embeddings_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
