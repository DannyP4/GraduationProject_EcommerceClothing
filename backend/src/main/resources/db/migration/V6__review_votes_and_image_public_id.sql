-- V6: helpful-vote table (one vote per user/review) + Cloudinary public_id on review images.
ALTER TABLE review_images
    ADD COLUMN public_id VARCHAR(255) DEFAULT NULL AFTER url;

CREATE TABLE review_helpful_votes (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    review_id  BIGINT UNSIGNED NOT NULL,
    user_id    BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_review_helpful (review_id, user_id),
    KEY idx_review_helpful_user (user_id),
    CONSTRAINT fk_review_helpful_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_helpful_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
