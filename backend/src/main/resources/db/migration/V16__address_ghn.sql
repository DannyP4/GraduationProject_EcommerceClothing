ALTER TABLE addresses
    ADD COLUMN ghn_province_id INT UNSIGNED NULL AFTER region,
    ADD COLUMN ghn_district_id INT UNSIGNED NULL AFTER ghn_province_id,
    ADD COLUMN ghn_ward_code   VARCHAR(20)  NULL AFTER ghn_district_id;
