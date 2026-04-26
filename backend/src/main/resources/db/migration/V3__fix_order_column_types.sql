-- Fix: @OrderColumn in Hibernate requires INTEGER, but V1 created SMALLINT.
-- This affects lookbook_products.sort_order used with @OrderColumn on Lookbook.products list.

ALTER TABLE lookbook_products
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0;
