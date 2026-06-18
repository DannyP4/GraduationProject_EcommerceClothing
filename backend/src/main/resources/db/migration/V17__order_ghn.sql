ALTER TABLE orders
    ADD COLUMN ghn_order_code  VARCHAR(50)  NULL AFTER shipping_region,
    ADD COLUMN ghn_district_id INT UNSIGNED NULL AFTER ghn_order_code,
    ADD COLUMN ghn_ward_code   VARCHAR(20)  NULL AFTER ghn_district_id;
