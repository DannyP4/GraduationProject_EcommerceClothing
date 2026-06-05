-- V7: Product-level sale (percent or fixed amount, optional time window)
ALTER TABLE products
    ADD COLUMN sale_type      ENUM('PERCENT','FIXED') DEFAULT NULL AFTER base_price,
    ADD COLUMN sale_value     DECIMAL(19,4)           DEFAULT NULL AFTER sale_type,
    ADD COLUMN sale_starts_at TIMESTAMP NULL          DEFAULT NULL AFTER sale_value,
    ADD COLUMN sale_ends_at   TIMESTAMP NULL          DEFAULT NULL AFTER sale_starts_at;

ALTER TABLE order_items
    ADD COLUMN original_unit_price DECIMAL(19,4) DEFAULT NULL AFTER unit_price;
