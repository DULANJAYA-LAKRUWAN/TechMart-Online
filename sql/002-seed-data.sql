-- =============================================================================
-- TechMart Online — Seed Data
-- =============================================================================

-- ── Roles ────────────────────────────────────────────────────────────────────

INSERT INTO roles (id, name, description) VALUES
    (1, 'ADMIN',           'Full system access — user management, inventory, orders'),
    (2, 'MANAGER',         'Operational management — orders, products, reports'),
    (3, 'CUSTOMER',        'Standard customer — browse, purchase, view own orders'),
    (4, 'WAREHOUSE_STAFF', 'Warehouse operations — inventory, shipping')
ON CONFLICT (id) DO NOTHING;

-- ── Admin User (password: admin123) ──────────────────────────────────────────

INSERT INTO users (id, username, email, password_hash, first_name, last_name, active)
VALUES (
    1,
    'admin',
    'admin@techmart.com',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    'System',
    'Admin',
    TRUE
) ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 1),
    (1, 2),
    (1, 3)
ON CONFLICT DO NOTHING;

-- ── Customer User (password: customer123) ────────────────────────────────────

INSERT INTO users (id, username, email, password_hash, first_name, last_name, active)
VALUES (
    2,
    'john.doe',
    'john@example.com',
    'a2c3e7a5f6b8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3',
    'John',
    'Doe',
    TRUE
) ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id) VALUES (2, 3)
ON CONFLICT DO NOTHING;

-- ── Categories ───────────────────────────────────────────────────────────────

INSERT INTO categories (id, name, description) VALUES
    (1, 'Electronics',    'Consumer electronics, gadgets, and accessories'),
    (2, 'Clothing',       'Apparel, fashion, and accessories'),
    (3, 'Home & Garden',  'Home improvement, furniture, and garden supplies'),
    (4, 'Sports',         'Sports equipment, fitness gear, and outdoors'),
    (5, 'Books',          'Books, e-books, and educational materials')
ON CONFLICT (id) DO NOTHING;

-- ── Products ─────────────────────────────────────────────────────────────────

INSERT INTO products (id, name, sku, description, price, category_id, brand, featured) VALUES
    (1001, 'Wireless Bluetooth Headphones', 'TM-EL-001', 'Noise-cancelling Bluetooth 5.3 headphones with 40hr battery', 149.99, 1, 'TechSound', TRUE),
    (1002, 'USB-C Hub 7-in-1',             'TM-EL-002', '7-port USB-C hub with HDMI 4K, USB 3.0, SD card reader', 49.99,  1, 'ConnectPro', TRUE),
    (1003, 'Mechanical Keyboard RGB',      'TM-EL-003', 'Cherry MX Blue switches, per-key RGB, aluminium frame', 129.99, 1, 'TypeMaster', FALSE),
    (1004, 'Cotton T-Shirt',               'TM-CL-001', 'Premium 100% organic cotton, slim fit', 29.99,  2, 'EcoWear', FALSE),
    (1005, 'Running Shoes',                'TM-CL-002', 'Lightweight mesh running shoes with responsive cushioning', 89.99,  2, 'RunFast', TRUE),
    (1006, 'Indoor Plant Pot Set',         'TM-HG-001', 'Set of 3 ceramic plant pots with drainage', 34.99,  3, 'GreenHome', FALSE),
    (1007, 'Yoga Mat Premium',             'TM-SP-001', '6mm thick non-slip yoga mat with carrying strap', 39.99,  4, 'FlexFit', TRUE),
    (1008, 'Resistance Bands Set',         'TM-SP-002', 'Set of 5 resistance bands with door anchor', 24.99,  4, 'FlexFit', FALSE)
ON CONFLICT (id) DO NOTHING;

-- ── Warehouses ───────────────────────────────────────────────────────────────

INSERT INTO warehouses (id, name, code, address, city, country, active) VALUES
    (1, 'Main Distribution Center', 'WH-NYC', '100 Industrial Blvd', 'New York', 'USA', TRUE),
    (2, 'West Coast Fulfillment',   'WH-LAX', '200 Warehouse Ave', 'Los Angeles', 'USA', TRUE),
    (3, 'Central Hub',              'WH-CHI', '300 Logistics Dr', 'Chicago', 'USA', TRUE)
ON CONFLICT (id) DO NOTHING;

-- ── Inventory ────────────────────────────────────────────────────────────────

INSERT INTO inventory (product_id, warehouse_id, quantity_in_stock, quantity_reserved) VALUES
    (1001, 1, 250, 10),
    (1001, 2, 180,  5),
    (1002, 1, 500, 20),
    (1002, 2, 300, 10),
    (1003, 1, 120,  5),
    (1004, 2, 800, 30),
    (1005, 2, 400, 15),
    (1005, 3, 200,  8),
    (1006, 1, 150,  5),
    (1007, 3, 300, 10),
    (1008, 3, 450, 12)
ON CONFLICT (product_id, warehouse_id) DO NOTHING;
