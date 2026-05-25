-- V4: Add `image_url` to categories for storefront banner / tile imagery.
ALTER TABLE categories
    ADD COLUMN image_url VARCHAR(500) DEFAULT NULL AFTER name;
