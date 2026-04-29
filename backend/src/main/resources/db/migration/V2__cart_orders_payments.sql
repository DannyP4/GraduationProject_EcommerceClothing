-- =============================================================================
-- V2: Cart, wishlist, orders, payments, promotions
-- =============================================================================
-- Tables: carts, cart_items, wishlists, orders, order_items,
--         order_status_history, payments, coupons, order_coupons
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- Cart & wishlist
-- -----------------------------------------------------------------------------

CREATE TABLE carts (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED DEFAULT NULL,
    session_id VARCHAR(64) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_carts_user    (user_id),
    KEY idx_carts_session (session_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cart_items (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cart_id    BIGINT UNSIGNED NOT NULL,
    variant_id BIGINT UNSIGNED NOT NULL,
    quantity   INT UNSIGNED NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_variant (cart_id, variant_id),
    KEY idx_cart_items_cart (cart_id),
    CONSTRAINT fk_cart_items_cart    FOREIGN KEY (cart_id)    REFERENCES carts (id)            ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wishlists (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wishlist_user_product (user_id, product_id),
    KEY idx_wishlists_user (user_id),
    CONSTRAINT fk_wishlists_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_wishlists_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Orders
-- -----------------------------------------------------------------------------

CREATE TABLE orders (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_number         VARCHAR(20) NOT NULL,
    user_id              BIGINT UNSIGNED NOT NULL,
    status               ENUM('PENDING','PAID','PROCESSING','SHIPPED','DELIVERED','CANCELLED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    subtotal             DECIMAL(19,4) NOT NULL,
    discount_total       DECIMAL(19,4) NOT NULL DEFAULT 0,
    shipping_cost        DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_total            DECIMAL(19,4) NOT NULL DEFAULT 0,
    grand_total          DECIMAL(19,4) NOT NULL,
    currency             CHAR(3) NOT NULL DEFAULT 'VND',
    -- Shipping address snapshot (denormalized at checkout time)
    shipping_recipient   VARCHAR(150) NOT NULL,
    shipping_phone       VARCHAR(20)  NOT NULL,
    shipping_line1       VARCHAR(255) NOT NULL,
    shipping_ward        VARCHAR(100) DEFAULT NULL,
    shipping_district    VARCHAR(100) NOT NULL,
    shipping_city        VARCHAR(100) NOT NULL,
    shipping_country     CHAR(2)      NOT NULL DEFAULT 'VN',
    shipping_postal_code VARCHAR(20)  DEFAULT NULL,
    notes                TEXT DEFAULT NULL,
    placed_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_number (order_number),
    KEY idx_orders_user      (user_id),
    KEY idx_orders_status    (status),
    KEY idx_orders_placed_at (placed_at),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id      BIGINT UNSIGNED NOT NULL,
    variant_id    BIGINT UNSIGNED NOT NULL,
    -- Snapshots taken at order time; immutable for invoice integrity
    product_name  VARCHAR(255) NOT NULL,
    variant_label VARCHAR(100) NOT NULL,
    sku           VARCHAR(64)  NOT NULL,
    unit_price    DECIMAL(19,4) NOT NULL,
    quantity      INT UNSIGNED NOT NULL,
    line_total    DECIMAL(19,4) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_items_order   (order_id),
    KEY idx_order_items_variant (variant_id),
    CONSTRAINT fk_order_items_order   FOREIGN KEY (order_id)   REFERENCES orders (id)           ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_status_history (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id           BIGINT UNSIGNED NOT NULL,
    status             ENUM('PENDING','PAID','PROCESSING','SHIPPED','DELIVERED','CANCELLED','REFUNDED') NOT NULL,
    note               VARCHAR(500) DEFAULT NULL,
    changed_by_user_id BIGINT UNSIGNED DEFAULT NULL,
    changed_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status_history_order (order_id),
    CONSTRAINT fk_status_history_order FOREIGN KEY (order_id)           REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_user  FOREIGN KEY (changed_by_user_id) REFERENCES users (id)  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Payments
-- -----------------------------------------------------------------------------

CREATE TABLE payments (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id        BIGINT UNSIGNED NOT NULL,
    provider        ENUM('COD','VNPAY','STRIPE','BANK_TRANSFER') NOT NULL,
    provider_txn_id VARCHAR(255) DEFAULT NULL,
    amount          DECIMAL(19,4) NOT NULL,
    currency        CHAR(3) NOT NULL DEFAULT 'VND',
    status          ENUM('PENDING','AUTHORIZED','CAPTURED','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    raw_request     JSON DEFAULT NULL,
    raw_response    JSON DEFAULT NULL,
    paid_at         TIMESTAMP NULL DEFAULT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_payments_order        (order_id),
    KEY idx_payments_provider_txn (provider, provider_txn_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Promotions
-- -----------------------------------------------------------------------------

CREATE TABLE coupons (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50) NOT NULL,
    type                ENUM('PERCENT','FIXED') NOT NULL,
    value               DECIMAL(19,4) NOT NULL,
    min_order_amount    DECIMAL(19,4) DEFAULT NULL,
    max_discount_amount DECIMAL(19,4) DEFAULT NULL,
    starts_at           TIMESTAMP NULL DEFAULT NULL,
    ends_at             TIMESTAMP NULL DEFAULT NULL,
    max_uses            INT UNSIGNED DEFAULT NULL,
    max_uses_per_user   INT UNSIGNED DEFAULT NULL,
    used_count          INT UNSIGNED NOT NULL DEFAULT 0,
    status              ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupons_code (code),
    KEY idx_coupons_active_dates (status, starts_at, ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_coupons (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id        BIGINT UNSIGNED NOT NULL,
    coupon_id       BIGINT UNSIGNED NOT NULL,
    discount_amount DECIMAL(19,4) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_coupon (order_id, coupon_id),
    KEY idx_order_coupons_coupon (coupon_id),
    CONSTRAINT fk_order_coupons_order  FOREIGN KEY (order_id)  REFERENCES orders (id)  ON DELETE CASCADE,
    CONSTRAINT fk_order_coupons_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
