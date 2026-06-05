-- V8: Coupon scope (whole-order / category / product)
ALTER TABLE coupons
    ADD COLUMN scope ENUM('ALL','CATEGORY','PRODUCT') NOT NULL DEFAULT 'ALL' AFTER `value`;

CREATE TABLE coupon_categories (
    coupon_id   BIGINT UNSIGNED NOT NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (coupon_id, category_id),
    KEY idx_coupon_categories_category (category_id),
    CONSTRAINT fk_coupon_categories_coupon   FOREIGN KEY (coupon_id)   REFERENCES coupons (id)    ON DELETE CASCADE,
    CONSTRAINT fk_coupon_categories_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE coupon_products (
    coupon_id  BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (coupon_id, product_id),
    KEY idx_coupon_products_product (product_id),
    CONSTRAINT fk_coupon_products_coupon  FOREIGN KEY (coupon_id)  REFERENCES coupons (id)  ON DELETE CASCADE,
    CONSTRAINT fk_coupon_products_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
