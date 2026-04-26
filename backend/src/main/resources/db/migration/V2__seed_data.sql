-- ============================================================
-- UNIFORM Student Streetwear — V2 Seed Data
-- Categories, products, variants, images matching UI designs
-- ============================================================

-- ── Categories ───────────────────────────────────────────────
INSERT INTO categories (id, parent_id, name, slug, sort_order, is_active) VALUES
(1,  NULL, 'New Arrivals',    'new-arrivals',    1, 1),
(2,  NULL, 'Semester Fits',   'semester-fits',   2, 1),
(3,  NULL, 'Accessories',     'accessories',     3, 1),
(4,  NULL, 'Sale',            'sale',            4, 1),
(5,  2,    'Outerwear',       'outerwear',       1, 1),
(6,  2,    'Tops',            'tops',            2, 1),
(7,  2,    'Bottoms',         'bottoms',         3, 1);

-- ── Products (matching homepage trending + PDP designs) ──────
INSERT INTO products (id, category_id, name, slug, description, material_specs,
                      base_price, compare_price, badge,
                      is_active, is_featured, is_vto_enabled,
                      avg_rating, review_count, total_sold) VALUES

-- 1. Campus Oversized Hoodie (PDP product — VTO enabled)
(1, 6, 'Campus Oversized Hoodie',
    'campus-oversized-hoodie',
    'The Campus Oversized Hoodie is your new semester staple. Heavyweight fleece construction with a relaxed silhouette designed for everything from early morning lectures to late-night study sessions.',
    '80% Cotton / 20% Polyester Fleece, 380gsm',
    89.00, NULL, 'HOT',
    1, 1, 1,
    4.80, 127, 843),

-- 2. Varsity Letter Tee (trending)
(2, 6, 'Varsity Letter Tee',
    'varsity-letter-tee',
    'Clean collegiate varsity graphics on our signature 200gsm heavyweight cotton. Pre-shrunk for a consistent fit wash after wash.',
    '100% Combed Ring-Spun Cotton, 200gsm',
    45.00, NULL, 'TRENDING',
    1, 1, 0,
    4.60, 89, 612),

-- 3. Utility Cargo Pants (trending)
(3, 7, 'Utility Cargo Pants',
    'utility-cargo-pants',
    'Six-pocket utility cargos built for campus life. Durable twill construction with reinforced knees and adjustable ankle cuffs.',
    '98% Cotton / 2% Elastane Twill, 280gsm',
    112.00, 140.00, 'SALE',
    1, 1, 0,
    4.40, 54, 389),

-- 4. Wool Blend Overcoat (trending, new arrival)
(4, 5, 'Wool Blend Overcoat',
    'wool-blend-overcoat',
    'Semester-ready outerwear. Our Wool Blend Overcoat features a structured silhouette that transitions effortlessly from campus walks to weekend plans.',
    '70% Wool / 25% Polyester / 5% Other',
    198.00, NULL, 'NEW_ARRIVAL',
    1, 1, 0,
    4.90, 31, 201),

-- 5. Ribbed Knit Beanie (accessories)
(5, 3, 'Ribbed Knit Beanie',
    'ribbed-knit-beanie',
    'Soft-touch ribbed beanie in our signature chunky knit. One size fits all.',
    '100% Merino Wool',
    28.00, NULL, 'NEW',
    1, 0, 0,
    4.70, 42, 278),

-- 6. Heavyweight Crewneck Sweatshirt
(6, 6, 'Heavyweight Crewneck Sweatshirt',
    'heavyweight-crewneck-sweatshirt',
    'The ultimate study-session sweatshirt. 400gsm French terry with a vintage-washed finish.',
    '80% Cotton / 20% Polyester French Terry, 400gsm',
    75.00, NULL, NULL,
    1, 0, 0,
    4.50, 68, 445),

-- 7. Wide-Leg Denim Jeans
(7, 7, 'Wide-Leg Denim Jeans',
    'wide-leg-denim-jeans',
    'Contemporary wide-leg cut in premium 12oz denim. Raw hem and clean finish.',
    '99% Cotton / 1% Elastane Denim, 12oz',
    98.00, 130.00, 'SALE',
    1, 0, 0,
    4.30, 37, 256);

-- ── Product Variants ─────────────────────────────────────────
-- Campus Oversized Hoodie (product_id=1) — colors: Washed Black, Oatmeal, Rust Red
INSERT INTO product_variants (product_id, size, color, sku, price_delta, stock_qty, low_stock_threshold) VALUES
(1, 'XS',  'Washed Black', 'COH-WBK-XS',  0.00,  8, 5),
(1, 'S',   'Washed Black', 'COH-WBK-S',   0.00, 24, 5),
(1, 'M',   'Washed Black', 'COH-WBK-M',   0.00, 32, 5),
(1, 'L',   'Washed Black', 'COH-WBK-L',   0.00, 20, 5),
(1, 'XL',  'Washed Black', 'COH-WBK-XL',  0.00, 12, 5),
(1, 'XXL', 'Washed Black', 'COH-WBK-XXL', 0.00,  4, 5),
(1, 'XS',  'Oatmeal',      'COH-OAT-XS',  0.00, 10, 5),
(1, 'S',   'Oatmeal',      'COH-OAT-S',   0.00, 28, 5),
(1, 'M',   'Oatmeal',      'COH-OAT-M',   0.00, 35, 5),
(1, 'L',   'Oatmeal',      'COH-OAT-L',   0.00, 22, 5),
(1, 'XL',  'Oatmeal',      'COH-OAT-XL',  0.00,  9, 5),
(1, 'XXL', 'Oatmeal',      'COH-OAT-XXL', 0.00,  3, 5),
(1, 'XS',  'Rust Red',     'COH-RST-XS',  0.00,  5, 5),
(1, 'S',   'Rust Red',     'COH-RST-S',   0.00, 18, 5),
(1, 'M',   'Rust Red',     'COH-RST-M',   0.00, 25, 5),
(1, 'L',   'Rust Red',     'COH-RST-L',   0.00, 14, 5),
(1, 'XL',  'Rust Red',     'COH-RST-XL',  0.00,  6, 5),
(1, 'XXL', 'Rust Red',     'COH-RST-XXL', 0.00,  2, 5),

-- Varsity Letter Tee (product_id=2)
(2, 'XS',  'White',   'VLT-WHT-XS',  0.00, 15, 5),
(2, 'S',   'White',   'VLT-WHT-S',   0.00, 30, 5),
(2, 'M',   'White',   'VLT-WHT-M',   0.00, 42, 5),
(2, 'L',   'White',   'VLT-WHT-L',   0.00, 28, 5),
(2, 'XL',  'White',   'VLT-WHT-XL',  0.00, 16, 5),
(2, 'S',   'Vintage Beige', 'VLT-VBG-S',  0.00, 20, 5),
(2, 'M',   'Vintage Beige', 'VLT-VBG-M',  0.00, 25, 5),
(2, 'L',   'Vintage Beige', 'VLT-VBG-L',  0.00, 18, 5),

-- Utility Cargo Pants (product_id=3)
(3, 'XS',  'Khaki',  'UCP-KHK-XS',  0.00,  6, 5),
(3, 'S',   'Khaki',  'UCP-KHK-S',   0.00, 14, 5),
(3, 'M',   'Khaki',  'UCP-KHK-M',   0.00, 20, 5),
(3, 'L',   'Khaki',  'UCP-KHK-L',   0.00, 15, 5),
(3, 'XL',  'Khaki',  'UCP-KHK-XL',  0.00,  8, 5),
(3, 'S',   'Olive',  'UCP-OLV-S',   0.00, 12, 5),
(3, 'M',   'Olive',  'UCP-OLV-M',   0.00, 18, 5),
(3, 'L',   'Olive',  'UCP-OLV-L',   0.00, 10, 5),

-- Wool Blend Overcoat (product_id=4)
(4, 'XS',  'Camel',  'WBO-CAM-XS',  0.00,  4, 3),
(4, 'S',   'Camel',  'WBO-CAM-S',   0.00,  8, 3),
(4, 'M',   'Camel',  'WBO-CAM-M',   0.00, 12, 3),
(4, 'L',   'Camel',  'WBO-CAM-L',   0.00,  9, 3),
(4, 'XL',  'Camel',  'WBO-CAM-XL',  0.00,  5, 3),
(4, 'S',   'Charcoal', 'WBO-CHR-S',  0.00,  6, 3),
(4, 'M',   'Charcoal', 'WBO-CHR-M',  0.00, 10, 3),
(4, 'L',   'Charcoal', 'WBO-CHR-L',  0.00,  7, 3),

-- Ribbed Knit Beanie (product_id=5)
(5, 'ONE_SIZE', 'Oatmeal',   'RKB-OAT-OS', 0.00, 45, 10),
(5, 'ONE_SIZE', 'Black',     'RKB-BLK-OS', 0.00, 38, 10),
(5, 'ONE_SIZE', 'Rust Red',  'RKB-RST-OS', 0.00, 22, 10),

-- Heavyweight Crewneck (product_id=6)
(6, 'S',  'Vintage Grey', 'HCS-VGY-S',  0.00, 20, 5),
(6, 'M',  'Vintage Grey', 'HCS-VGY-M',  0.00, 30, 5),
(6, 'L',  'Vintage Grey', 'HCS-VGY-L',  0.00, 22, 5),
(6, 'XL', 'Vintage Grey', 'HCS-VGY-XL', 0.00, 14, 5),
(6, 'S',  'Washed Black', 'HCS-WBK-S',  0.00, 18, 5),
(6, 'M',  'Washed Black', 'HCS-WBK-M',  0.00, 25, 5),
(6, 'L',  'Washed Black', 'HCS-WBK-L',  0.00, 16, 5),

-- Wide-Leg Denim (product_id=7)
(7, 'XS', 'Light Wash', 'WLD-LWS-XS', 0.00,  5, 5),
(7, 'S',  'Light Wash', 'WLD-LWS-S',  0.00, 12, 5),
(7, 'M',  'Light Wash', 'WLD-LWS-M',  0.00, 16, 5),
(7, 'L',  'Light Wash', 'WLD-LWS-L',  0.00, 10, 5),
(7, 'S',  'Dark Wash',  'WLD-DWS-S',  0.00,  9, 5),
(7, 'M',  'Dark Wash',  'WLD-DWS-M',  0.00, 14, 5),
(7, 'L',  'Dark Wash',  'WLD-DWS-L',  0.00,  8, 5);

-- ── Product Images ────────────────────────────────────────────
-- Using Unsplash placeholder paths — replace with real S3 URLs
INSERT INTO product_images (product_id, url, alt_text, sort_order, is_primary) VALUES
-- Campus Oversized Hoodie
(1, 'https://images.unsplash.com/photo-1556821840-3a63f15232d0?w=800', 'Campus Oversized Hoodie - Front', 0, 1),
(1, 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800', 'Campus Oversized Hoodie - Back',  1, 0),
(1, 'https://images.unsplash.com/photo-1590330297626-d7aff25a0431?w=800', 'Campus Oversized Hoodie - Detail', 2, 0),
(1, 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800', 'Campus Oversized Hoodie - Flat Lay', 3, 0),
-- Varsity Letter Tee
(2, 'https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=800', 'Varsity Letter Tee - Front', 0, 1),
(2, 'https://images.unsplash.com/photo-1529374255404-311a2a4f1fd9?w=800', 'Varsity Letter Tee - Back',  1, 0),
-- Utility Cargo Pants
(3, 'https://images.unsplash.com/photo-1594938298603-c8148c4b2d47?w=800', 'Utility Cargo Pants - Front', 0, 1),
(3, 'https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=800', 'Utility Cargo Pants - Side',  1, 0),
-- Wool Blend Overcoat
(4, 'https://images.unsplash.com/photo-1544022613-e87ca75a784a?w=800', 'Wool Blend Overcoat - Front', 0, 1),
(4, 'https://images.unsplash.com/photo-1548883354-94bcfe321cbb?w=800', 'Wool Blend Overcoat - Back',  1, 0),
-- Ribbed Knit Beanie
(5, 'https://images.unsplash.com/photo-1576871337622-98d48d1cf531?w=800', 'Ribbed Knit Beanie', 0, 1),
-- Heavyweight Crewneck
(6, 'https://images.unsplash.com/photo-1509942774463-acf339cf87d5?w=800', 'Heavyweight Crewneck - Front', 0, 1),
(6, 'https://images.unsplash.com/photo-1618354691438-25bc04584c23?w=800', 'Heavyweight Crewneck - Back',  1, 0),
-- Wide-Leg Denim
(7, 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=800', 'Wide-Leg Denim - Front', 0, 1),
(7, 'https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800', 'Wide-Leg Denim - Back',  1, 0);

-- ── "Complete The Fit" related products ───────────────────────
INSERT INTO product_related (product_id, related_id, sort_order) VALUES
(1, 3, 1), -- Hoodie → Cargo Pants
(1, 2, 2), -- Hoodie → Varsity Tee
(1, 5, 3), -- Hoodie → Beanie
(4, 6, 1), -- Overcoat → Crewneck
(4, 7, 2), -- Overcoat → Wide-Leg Denim
(4, 3, 3); -- Overcoat → Cargo Pants

-- ── Lookbooks (home page editorial grid) ─────────────────────
INSERT INTO lookbooks (id, title, tag, slug, cover_url, grid_size, is_featured, sort_order, is_active) VALUES
(1, 'Late Nights',        'Semester Essentials',  'late-nights',
    'https://images.unsplash.com/photo-1523398002811-999ca8dec234?w=800',
    'LARGE', 1, 1, 1),
(2, 'Dorm Life',          'Casual Fits',          'dorm-life',
    'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=800',
    'SMALL', 1, 2, 1),
(3, 'Campus to Coffee',   'Weekend Vibes',        'campus-to-coffee',
    'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=800',
    'SMALL', 1, 3, 1),
(4, 'Semester Staples',   'Essentials',           'semester-staples',
    'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=800',
    'HALF',  0, 4, 1),
(5, 'Study Session',      'Cozy Fits',            'study-session',
    'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=800',
    'HALF',  0, 5, 1);

-- ── Lookbook ↔ Product associations ─────────────────────────
INSERT INTO lookbook_products (lookbook_id, product_id, sort_order) VALUES
(1, 1, 1), (1, 3, 2), (1, 5, 3),  -- Late Nights: Hoodie, Cargo Pants, Beanie
(2, 2, 1), (2, 6, 2), (2, 7, 3),  -- Dorm Life: Varsity Tee, Crewneck, Denim
(3, 4, 1), (3, 3, 2),             -- Campus to Coffee: Overcoat, Cargo Pants
(4, 1, 1), (4, 6, 2), (4, 5, 3), -- Semester Staples: Hoodie, Crewneck, Beanie
(5, 2, 1), (5, 6, 2), (5, 7, 3); -- Study Session: Tee, Crewneck, Denim

-- ── Coupons ──────────────────────────────────────────────────
INSERT INTO coupons (code, description, discount_type, discount_value, minimum_order_amount,
                     maximum_discount_cap, is_student_only, usage_limit_total, usage_limit_per_user,
                     is_active, valid_from, valid_until) VALUES
-- General discount shown in cart UI
('UNIFORM15',   '15% off your order',
    'PERCENTAGE', 15.00, 50.00,  30.00, 0, NULL, 3, 1,
    '2024-01-01 00:00:00', '2026-12-31 23:59:59'),
-- Student-only discount
('STUDENT20',   '20% off for verified students',
    'PERCENTAGE', 20.00, 0.00,   50.00, 1, NULL, 5, 1,
    '2024-01-01 00:00:00', '2026-12-31 23:59:59'),
-- Fixed amount for new arrivals
('NEWFIT10',    '$10 off any order over $75',
    'FIXED_AMOUNT', 10.00, 75.00, NULL,  0, 500,  1, 1,
    '2024-09-01 00:00:00', '2025-05-31 23:59:59'),
-- Welcome code for new registrations
('WELCOME',     'Welcome gift — $5 off your first order',
    'FIXED_AMOUNT', 5.00, 0.00,  NULL,   0, NULL, 1, 1,
    '2024-01-01 00:00:00', NULL);
