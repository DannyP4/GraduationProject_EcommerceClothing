-- ============================================================
-- UNIFORM Student Streetwear — V1 Schema
-- Engine: InnoDB | Charset: utf8mb4_unicode_ci
-- ============================================================

-- ── 1. users ─────────────────────────────────────────────────
CREATE TABLE users (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email            VARCHAR(255)    NOT NULL,
    password_hash    VARCHAR(255)    NULL COMMENT 'NULL = OAuth-only account',
    first_name       VARCHAR(100)    NOT NULL,
    last_name        VARCHAR(100)    NOT NULL,
    phone            VARCHAR(30)     NULL,
    avatar_url       VARCHAR(2048)   NULL,
    role             ENUM('CUSTOMER','ADMIN','MODERATOR') NOT NULL DEFAULT 'CUSTOMER',
    is_student       TINYINT(1)      NOT NULL DEFAULT 0,
    student_email    VARCHAR(255)    NULL COMMENT 'e.g. name@university.edu',
    student_verified TINYINT(1)      NOT NULL DEFAULT 0,
    is_active        TINYINT(1)      NOT NULL DEFAULT 1,
    email_verified   TINYINT(1)      NOT NULL DEFAULT 0,
    last_login_at    DATETIME(6)     NULL,
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at       DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    INDEX idx_users_role (role),
    INDEX idx_users_is_student (is_student)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 2. oauth_providers ───────────────────────────────────────
CREATE TABLE oauth_providers (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    provider         ENUM('GOOGLE','FACEBOOK','TWITTER') NOT NULL,
    provider_user_id VARCHAR(255)    NOT NULL,
    access_token     TEXT            NULL COMMENT 'Encrypt before storing',
    refresh_token    TEXT            NULL,
    token_expires_at DATETIME(6)     NULL,
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_oauth_provider_user (provider, provider_user_id),
    INDEX idx_oauth_user_id (user_id),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 3. categories ────────────────────────────────────────────
CREATE TABLE categories (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT UNSIGNED NULL,
    name        VARCHAR(100)    NOT NULL,
    slug        VARCHAR(120)    NOT NULL,
    description TEXT            NULL,
    image_url   VARCHAR(2048)   NULL,
    sort_order  SMALLINT        NOT NULL DEFAULT 0,
    is_active   TINYINT(1)      NOT NULL DEFAULT 1,
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_slug (slug),
    INDEX idx_categories_parent (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 4. products ──────────────────────────────────────────────
CREATE TABLE products (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id      BIGINT UNSIGNED NOT NULL,
    name             VARCHAR(255)    NOT NULL,
    slug             VARCHAR(280)    NOT NULL,
    description      TEXT            NULL,
    material_specs   TEXT            NULL COMMENT 'e.g. "80% Wool / 20% Poly"',
    base_price       DECIMAL(10,2)   NOT NULL,
    compare_price    DECIMAL(10,2)   NULL COMMENT 'Crossed-out price for sale items',
    badge            ENUM('HOT','NEW','NEW_ARRIVAL','SALE','TRENDING') NULL,
    is_active        TINYINT(1)      NOT NULL DEFAULT 1,
    is_featured      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT 'Shows in homepage trending section',
    is_vto_enabled   TINYINT(1)      NOT NULL DEFAULT 0 COMMENT 'Enables Virtual Try-On button on PDP',
    vto_model_url    VARCHAR(2048)   NULL COMMENT 'S3 AR overlay image URL',
    total_sold       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT 'Denormalized for sort-by-popularity',
    avg_rating       DECIMAL(3,2)    NOT NULL DEFAULT 0.00 COMMENT 'Denormalized; updated on review change',
    review_count     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT 'Denormalized',
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at       DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_products_slug (slug),
    INDEX idx_products_category (category_id),
    INDEX idx_products_badge (badge),
    INDEX idx_products_featured (is_featured),
    INDEX idx_products_active_price (is_active, base_price),
    FULLTEXT INDEX ft_products_search (name, description),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 5. product_variants ──────────────────────────────────────
CREATE TABLE product_variants (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id          BIGINT UNSIGNED NOT NULL,
    size                ENUM('XS','S','M','L','XL','XXL','ONE_SIZE') NOT NULL,
    color               VARCHAR(60)     NOT NULL COMMENT 'e.g. "Rust Red", "Oatmeal"',
    sku                 VARCHAR(100)    NOT NULL,
    price_delta         DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT 'Surcharge added to base_price',
    stock_qty           INT UNSIGNED    NOT NULL DEFAULT 0,
    low_stock_threshold SMALLINT        NOT NULL DEFAULT 5,
    is_active           TINYINT(1)      NOT NULL DEFAULT 1,
    version             BIGINT          NOT NULL DEFAULT 0 COMMENT 'Optimistic locking for stock safety',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_variant_sku (sku),
    UNIQUE KEY uq_variant_product_size_color (product_id, size, color),
    INDEX idx_variant_stock (stock_qty),
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 6. product_images ────────────────────────────────────────
CREATE TABLE product_images (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id  BIGINT UNSIGNED NOT NULL,
    url         VARCHAR(2048)   NOT NULL,
    alt_text    VARCHAR(255)    NULL,
    sort_order  SMALLINT        NOT NULL DEFAULT 0 COMMENT '0 = primary thumbnail',
    is_primary  TINYINT(1)      NOT NULL DEFAULT 0,
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_prodimg_sort (product_id, sort_order),
    CONSTRAINT fk_prodimg_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 7. product_related ───────────────────────────────────────
CREATE TABLE product_related (
    product_id   BIGINT UNSIGNED NOT NULL,
    related_id   BIGINT UNSIGNED NOT NULL,
    sort_order   SMALLINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, related_id),
    CONSTRAINT fk_related_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_related_target  FOREIGN KEY (related_id)  REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 8. product_reviews ───────────────────────────────────────
-- Note: fk_review_orderitem references order_items created below
CREATE TABLE product_reviews (
    id                   BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    product_id           BIGINT UNSIGNED  NOT NULL,
    user_id              BIGINT UNSIGNED  NOT NULL,
    order_item_id        BIGINT UNSIGNED  NULL COMMENT 'Links to purchase for verified badge',
    rating               TINYINT UNSIGNED NOT NULL,
    title                VARCHAR(200)     NULL,
    body                 TEXT             NULL,
    is_verified_purchase TINYINT(1)       NOT NULL DEFAULT 0,
    is_approved          TINYINT(1)       NOT NULL DEFAULT 1,
    helpful_count        INT UNSIGNED     NOT NULL DEFAULT 0,
    created_at           DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at           DATETIME(6)      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_review_user_product (user_id, product_id),
    INDEX idx_review_product_approved (product_id, is_approved),
    INDEX idx_review_rating (product_id, rating),
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 9. wishlist_items ────────────────────────────────────────
CREATE TABLE wishlist_items (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED NOT NULL,
    variant_id  BIGINT UNSIGNED NULL COMMENT 'Optional: saved with specific size/color',
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_wishlist_user_product_variant (user_id, product_id, variant_id),
    INDEX idx_wishlist_user (user_id),
    CONSTRAINT fk_wish_user    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wish_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_wish_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 10. coupons ──────────────────────────────────────────────
-- Created before carts (carts.coupon_id FK → coupons)
CREATE TABLE coupons (
    id                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    code                  VARCHAR(50)       NOT NULL,
    description           VARCHAR(255)      NULL,
    discount_type         ENUM('PERCENTAGE','FIXED_AMOUNT') NOT NULL,
    discount_value        DECIMAL(10,2)     NOT NULL,
    minimum_order_amount  DECIMAL(10,2)     NOT NULL DEFAULT 0.00,
    maximum_discount_cap  DECIMAL(10,2)     NULL COMMENT 'Max $ off for % codes; NULL = uncapped',
    is_student_only       TINYINT(1)        NOT NULL DEFAULT 0,
    usage_limit_total     INT UNSIGNED      NULL COMMENT 'NULL = unlimited',
    usage_limit_per_user  SMALLINT UNSIGNED NULL,
    times_used            INT UNSIGNED      NOT NULL DEFAULT 0 COMMENT 'Denormalized counter',
    is_active             TINYINT(1)        NOT NULL DEFAULT 1,
    valid_from            DATETIME(6)       NOT NULL,
    valid_until           DATETIME(6)       NULL,
    created_at            DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at            DATETIME(6)       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_coupon_code (code),
    INDEX idx_coupon_active_dates (is_active, valid_from, valid_until),
    INDEX idx_coupon_student (is_student_only)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 11. carts ────────────────────────────────────────────────
CREATE TABLE carts (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id       BIGINT UNSIGNED NULL COMMENT 'NULL = guest cart',
    session_token VARCHAR(128)    NULL COMMENT 'UUID for guest; merged on login',
    coupon_id     BIGINT UNSIGNED NULL,
    created_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    expires_at    DATETIME(6)     NULL COMMENT 'Guest carts expire after 30 days idle',
    PRIMARY KEY (id),
    UNIQUE KEY uq_cart_user (user_id),
    UNIQUE KEY uq_cart_session (session_token),
    INDEX idx_cart_expires (expires_at),
    CONSTRAINT fk_cart_user   FOREIGN KEY (user_id)   REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_coupon FOREIGN KEY (coupon_id)  REFERENCES coupons(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 12. cart_items ───────────────────────────────────────────
CREATE TABLE cart_items (
    id                 BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    cart_id            BIGINT UNSIGNED   NOT NULL,
    variant_id         BIGINT UNSIGNED   NOT NULL,
    quantity           SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    is_saved_for_later TINYINT(1)        NOT NULL DEFAULT 0,
    created_at         DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_cartitem_cart_variant (cart_id, variant_id),
    INDEX idx_cartitem_cart (cart_id),
    CONSTRAINT fk_cartitem_cart    FOREIGN KEY (cart_id)    REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cartitem_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 13. addresses ────────────────────────────────────────────
CREATE TABLE addresses (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL,
    label          VARCHAR(60)     NULL COMMENT 'e.g. "Home", "Dorm Room 204"',
    recipient_name VARCHAR(200)    NOT NULL,
    phone          VARCHAR(30)     NULL,
    address_line1  VARCHAR(255)    NOT NULL,
    address_line2  VARCHAR(255)    NULL,
    city           VARCHAR(100)    NOT NULL,
    state          VARCHAR(100)    NULL,
    postal_code    VARCHAR(20)     NOT NULL,
    country_code   CHAR(2)         NOT NULL DEFAULT 'US',
    is_default     TINYINT(1)      NOT NULL DEFAULT 0,
    created_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at     DATETIME(6)     NULL,
    PRIMARY KEY (id),
    INDEX idx_address_user_default (user_id, is_default),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 14. orders ───────────────────────────────────────────────
CREATE TABLE orders (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id              BIGINT UNSIGNED NOT NULL,
    order_number         VARCHAR(30)     NOT NULL COMMENT 'e.g. UNI-20240315-00042',
    status               ENUM('PENDING_PAYMENT','PAID','PROCESSING','SHIPPED','DELIVERED','CANCELLED','REFUNDED') NOT NULL DEFAULT 'PENDING_PAYMENT',
    -- Shipping address snapshot
    shipping_name        VARCHAR(200)    NOT NULL,
    shipping_line1       VARCHAR(255)    NOT NULL,
    shipping_line2       VARCHAR(255)    NULL,
    shipping_city        VARCHAR(100)    NOT NULL,
    shipping_state       VARCHAR(100)    NULL,
    shipping_postal      VARCHAR(20)     NOT NULL,
    shipping_country     CHAR(2)         NOT NULL,
    -- Financials
    subtotal             DECIMAL(10,2)   NOT NULL,
    shipping_fee         DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    tax_amount           DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    discount_amount      DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    total_amount         DECIMAL(10,2)   NOT NULL,
    coupon_id            BIGINT UNSIGNED NULL,
    coupon_code_snapshot VARCHAR(50)     NULL COMMENT 'Snapshot in case coupon is deleted later',
    notes                TEXT            NULL,
    tracking_number      VARCHAR(100)    NULL,
    tracking_carrier     VARCHAR(60)     NULL,
    paid_at              DATETIME(6)     NULL,
    shipped_at           DATETIME(6)     NULL,
    delivered_at         DATETIME(6)     NULL,
    cancelled_at         DATETIME(6)     NULL,
    created_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_order_number (order_number),
    INDEX idx_order_user_status (user_id, status),
    INDEX idx_order_created (created_at),
    CONSTRAINT fk_order_user   FOREIGN KEY (user_id)   REFERENCES users(id),
    CONSTRAINT fk_order_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 15. order_items ──────────────────────────────────────────
CREATE TABLE order_items (
    id                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    order_id              BIGINT UNSIGNED   NOT NULL,
    variant_id            BIGINT UNSIGNED   NULL COMMENT 'SET NULL if variant deleted; snapshots remain',
    -- Snapshot columns
    product_id_snapshot   BIGINT UNSIGNED   NOT NULL,
    product_name_snapshot VARCHAR(255)      NOT NULL,
    product_slug_snapshot VARCHAR(280)      NOT NULL,
    sku_snapshot          VARCHAR(100)      NOT NULL,
    size_snapshot         VARCHAR(20)       NOT NULL,
    color_snapshot        VARCHAR(60)       NOT NULL,
    image_url_snapshot    VARCHAR(2048)     NULL,
    unit_price            DECIMAL(10,2)     NOT NULL COMMENT 'Effective price at purchase time',
    quantity              SMALLINT UNSIGNED NOT NULL,
    line_total            DECIMAL(10,2)     NOT NULL COMMENT 'unit_price * quantity',
    created_at            DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_orderitem_order (order_id),
    INDEX idx_orderitem_product_snap (product_id_snapshot),
    CONSTRAINT fk_orderitem_order   FOREIGN KEY (order_id)   REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_orderitem_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add deferred FK on product_reviews → order_items (now that order_items exists)
ALTER TABLE product_reviews
    ADD CONSTRAINT fk_review_orderitem
        FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE SET NULL;

-- ── 16. coupon_usages ────────────────────────────────────────
CREATE TABLE coupon_usages (
    id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT UNSIGNED NOT NULL,
    user_id   BIGINT UNSIGNED NOT NULL,
    order_id  BIGINT UNSIGNED NOT NULL,
    used_at   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_coupon_usage_user_coupon (user_id, coupon_id),
    CONSTRAINT fk_usage_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT fk_usage_user   FOREIGN KEY (user_id)   REFERENCES users(id),
    CONSTRAINT fk_usage_order  FOREIGN KEY (order_id)  REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 17. payments ─────────────────────────────────────────────
CREATE TABLE payments (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id           BIGINT UNSIGNED NOT NULL,
    gateway            ENUM('STRIPE','PAYPAL','COD','MOMO','VNPAY') NOT NULL,
    gateway_payment_id VARCHAR(255)    NOT NULL,
    gateway_charge_id  VARCHAR(255)    NULL,
    amount             DECIMAL(10,2)   NOT NULL,
    currency           CHAR(3)         NOT NULL DEFAULT 'USD',
    status             ENUM('PENDING','AUTHORIZED','CAPTURED','FAILED','REFUNDED','PARTIALLY_REFUNDED') NOT NULL DEFAULT 'PENDING',
    method             VARCHAR(50)     NULL COMMENT 'e.g. "VISA ending 4242"',
    gateway_response   JSON            NULL COMMENT 'Full raw response for auditing',
    refunded_amount    DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    created_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_payment_order (order_id),
    INDEX idx_payment_gateway_id (gateway_payment_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 18. vto_sessions ─────────────────────────────────────────
CREATE TABLE vto_sessions (
    id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id       BIGINT UNSIGNED  NOT NULL,
    product_id    BIGINT UNSIGNED  NOT NULL,
    variant_id    BIGINT UNSIGNED  NULL,
    status        ENUM('ACTIVE','COMPLETED','ABANDONED') NOT NULL DEFAULT 'ACTIVE',
    view_mode     ENUM('FRONT','BACK') NOT NULL DEFAULT 'FRONT',
    opacity_value TINYINT UNSIGNED NOT NULL DEFAULT 100 COMMENT '0-100 matches slider in UI',
    added_to_cart TINYINT(1)       NOT NULL DEFAULT 0 COMMENT 'Conversion tracking',
    started_at    DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ended_at      DATETIME(6)      NULL,
    created_at    DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_vto_user (user_id),
    INDEX idx_vto_product (product_id),
    CONSTRAINT fk_vto_user    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_vto_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_vto_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 19. vto_photos ───────────────────────────────────────────
CREATE TABLE vto_photos (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    vto_session_id BIGINT UNSIGNED NOT NULL,
    user_id        BIGINT UNSIGNED NOT NULL,
    product_id     BIGINT UNSIGNED NOT NULL,
    s3_key         VARCHAR(512)    NOT NULL COMMENT 'Object key in S3/cloud storage',
    s3_url         VARCHAR(2048)   NOT NULL COMMENT 'CDN URL for display',
    view_mode      ENUM('FRONT','BACK') NOT NULL DEFAULT 'FRONT',
    is_shared      TINYINT(1)      NOT NULL DEFAULT 0,
    share_token    VARCHAR(64)     NULL COMMENT 'UUID for public share link /try-on/share/{token}',
    captured_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at     DATETIME(6)     NULL COMMENT 'Triggers async S3 cleanup job when set',
    PRIMARY KEY (id),
    INDEX idx_vtophoto_session (vto_session_id),
    INDEX idx_vtophoto_user_shared (user_id, is_shared),
    INDEX idx_vtophoto_token (share_token),
    CONSTRAINT fk_vtophoto_session FOREIGN KEY (vto_session_id) REFERENCES vto_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_vtophoto_user    FOREIGN KEY (user_id)        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_vtophoto_product FOREIGN KEY (product_id)     REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 20. lookbooks ────────────────────────────────────────────
CREATE TABLE lookbooks (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title       VARCHAR(200)    NOT NULL COMMENT 'e.g. "Late Nights", "Dorm Life"',
    tag         VARCHAR(80)     NULL COMMENT 'e.g. "Semester Essentials"',
    slug        VARCHAR(230)    NOT NULL,
    cover_url   VARCHAR(2048)   NOT NULL,
    description TEXT            NULL,
    grid_size   ENUM('LARGE','SMALL','HALF') NOT NULL DEFAULT 'HALF' COMMENT 'Maps to c-large/c-small/c-half CSS grid classes',
    is_featured TINYINT(1)      NOT NULL DEFAULT 0,
    sort_order  SMALLINT        NOT NULL DEFAULT 0,
    is_active   TINYINT(1)      NOT NULL DEFAULT 1,
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_lookbook_slug (slug),
    INDEX idx_lookbook_featured_sort (is_featured, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 21. lookbook_products ────────────────────────────────────
CREATE TABLE lookbook_products (
    lookbook_id BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED NOT NULL,
    sort_order  SMALLINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (lookbook_id, product_id),
    CONSTRAINT fk_lbp_lookbook FOREIGN KEY (lookbook_id) REFERENCES lookbooks(id) ON DELETE CASCADE,
    CONSTRAINT fk_lbp_product  FOREIGN KEY (product_id)  REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 22. notifications ────────────────────────────────────────
CREATE TABLE notifications (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    type       ENUM('ORDER_SHIPPED','ORDER_DELIVERED','BACK_IN_STOCK','PROMO_CODE','REVIEW_APPROVED') NOT NULL,
    title      VARCHAR(200)    NOT NULL,
    body       TEXT            NULL,
    link_url   VARCHAR(1024)   NULL,
    is_read    TINYINT(1)      NOT NULL DEFAULT 0,
    created_at DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at    DATETIME(6)     NULL,
    PRIMARY KEY (id),
    INDEX idx_notif_user_unread (user_id, is_read, created_at),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
