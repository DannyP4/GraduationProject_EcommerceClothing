-- V9: Region-tier shipping
ALTER TABLE addresses
    ADD COLUMN region ENUM('NORTH','CENTRAL','SOUTH') NULL AFTER postal_code;

ALTER TABLE orders
    ADD COLUMN shipping_region ENUM('NORTH','CENTRAL','SOUTH') NULL AFTER shipping_postal_code;
