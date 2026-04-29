-- =============================================================================
-- V3: Reviews, audit log, product views (recommendation seed data)
-- =============================================================================
-- Tables: reviews, review_images, audit_log, product_views
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- Reviews
-- -----------------------------------------------------------------------------

CREATE TABLE reviews (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL,
    product_id        BIGINT UNSIGNED NOT NULL,
    variant_id        BIGINT UNSIGNED DEFAULT NULL,
    order_id          BIGINT UNSIGNED DEFAULT NULL,
    rating            TINYINT UNSIGNED NOT NULL,
    title             VARCHAR(255) DEFAULT NULL,
    body              TEXT DEFAULT NULL,
    verified_purchase BOOLEAN NOT NULL DEFAULT FALSE,
    helpful_count     INT UNSIGNED NOT NULL DEFAULT 0,
    status            ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_reviews_product (product_id),
    KEY idx_reviews_user    (user_id),
    KEY idx_reviews_status  (status),
    KEY idx_reviews_variant (variant_id),
    KEY idx_reviews_order   (order_id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_reviews_user    FOREIGN KEY (user_id)    REFERENCES users (id)            ON DELETE CASCADE,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id)         ON DELETE CASCADE,
    CONSTRAINT fk_reviews_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE SET NULL,
    CONSTRAINT fk_reviews_order   FOREIGN KEY (order_id)   REFERENCES orders (id)           ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_images (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    review_id  BIGINT UNSIGNED NOT NULL,
    url        VARCHAR(500) NOT NULL,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_review_images_review (review_id),
    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Audit log (polymorphic — replaces vague "manage" relationship)
-- -----------------------------------------------------------------------------

CREATE TABLE audit_log (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT UNSIGNED DEFAULT NULL,
    entity_type   VARCHAR(50)  NOT NULL,
    entity_id     BIGINT UNSIGNED NOT NULL,
    action        VARCHAR(50)  NOT NULL,
    diff          JSON DEFAULT NULL,
    ip_address    VARCHAR(45)  DEFAULT NULL,
    user_agent    VARCHAR(255) DEFAULT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_entity  (entity_type, entity_id),
    KEY idx_audit_actor   (actor_user_id),
    KEY idx_audit_created (created_at),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Product views (seed data for future recommendation engine)
-- -----------------------------------------------------------------------------

CREATE TABLE product_views (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED DEFAULT NULL,
    session_id VARCHAR(64) DEFAULT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    viewed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_views_product_time (product_id, viewed_at),
    KEY idx_views_user_time    (user_id, viewed_at),
    KEY idx_views_session      (session_id),
    CONSTRAINT fk_views_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE SET NULL,
    CONSTRAINT fk_views_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
