-- =============================================================================
-- TechMart Online — Database Schema
-- Target: PostgreSQL 14+
-- =============================================================================

-- ── Sequences (allocationSize = 1 matching JPA @SequenceGenerator) ───────────

CREATE SEQUENCE IF NOT EXISTS users_id_seq          START 1000 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS roles_id_seq           START 100 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS categories_id_seq      START 100 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS products_id_seq        START 10000 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS orders_id_seq          START 50000 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS order_items_id_seq     START 50000 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS inventory_id_seq       START 1000 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS warehouses_id_seq      START 10 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS notifications_id_seq   START 10000 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS audit_logs_id_seq      START 10000 INCREMENT 10;

-- ── Roles ────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('roles_id_seq'),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);

-- ── Users ────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       PRIMARY KEY DEFAULT nextval('users_id_seq'),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    phone         VARCHAR(20),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    version       BIGINT       DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_active   ON users(active);

-- ── User-Roles Join Table ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ── Categories ───────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('categories_id_seq'),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url   VARCHAR(255),
    parent_id   BIGINT       REFERENCES categories(id),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_categories_name      ON categories(name);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_active    ON categories(active);

-- ── Products ─────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS products (
    id            BIGINT        PRIMARY KEY DEFAULT nextval('products_id_seq'),
    name          VARCHAR(200)  NOT NULL,
    sku           VARCHAR(50)   NOT NULL UNIQUE,
    description   VARCHAR(2000),
    price         NUMERIC(12,2) NOT NULL,
    compare_price NUMERIC(12,2),
    image_url     VARCHAR(500),
    brand         VARCHAR(100),
    weight_kg     NUMERIC(8,3),
    featured      BOOLEAN       NOT NULL DEFAULT FALSE,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    category_id   BIGINT        REFERENCES categories(id),
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    version       BIGINT        DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_products_sku      ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_active   ON products(active);
CREATE INDEX IF NOT EXISTS idx_products_price    ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_name     ON products(name);

-- ── Warehouses ───────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS warehouses (
    id             BIGINT       PRIMARY KEY DEFAULT nextval('warehouses_id_seq'),
    name           VARCHAR(100) NOT NULL,
    code           VARCHAR(20)  NOT NULL UNIQUE,
    address        VARCHAR(255) NOT NULL,
    city           VARCHAR(100) NOT NULL,
    country        VARCHAR(100) NOT NULL,
    postal_code    VARCHAR(20),
    contact_email  VARCHAR(100),
    contact_phone  VARCHAR(20),
    capacity       INTEGER,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_warehouses_code    ON warehouses(code);
CREATE INDEX IF NOT EXISTS idx_warehouses_active  ON warehouses(active);
CREATE INDEX IF NOT EXISTS idx_warehouses_country ON warehouses(country);

-- ── Inventory ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS inventory (
    id                  BIGINT       PRIMARY KEY DEFAULT nextval('inventory_id_seq'),
    product_id          BIGINT       NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    warehouse_id        BIGINT       NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    quantity_in_stock   INTEGER      NOT NULL DEFAULT 0,
    quantity_reserved   INTEGER      NOT NULL DEFAULT 0,
    reorder_threshold   INTEGER      NOT NULL DEFAULT 10,
    reorder_quantity    INTEGER      DEFAULT 50,
    last_restocked_at   TIMESTAMP,
    updated_at          TIMESTAMP,
    version             BIGINT       DEFAULT 0,
    CONSTRAINT uq_inventory_product_warehouse UNIQUE (product_id, warehouse_id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_product   ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warehouse ON inventory(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_quantity  ON inventory(quantity_in_stock);

-- ── Orders ───────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS orders (
    id                BIGINT        PRIMARY KEY DEFAULT nextval('orders_id_seq'),
    order_number      VARCHAR(50)   NOT NULL UNIQUE,
    user_id           BIGINT        NOT NULL REFERENCES users(id),
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    total_amount      NUMERIC(14,2) NOT NULL DEFAULT 0,
    shipping_amount   NUMERIC(10,2) DEFAULT 0,
    tax_amount        NUMERIC(10,2) DEFAULT 0,
    shipping_address  VARCHAR(500),
    billing_address   VARCHAR(500),
    payment_method    VARCHAR(50),
    payment_reference VARCHAR(100),
    notes             VARCHAR(1000),
    shipped_at        TIMESTAMP,
    delivered_at      TIMESTAMP,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    version           BIGINT        DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id    ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_order_no   ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

-- ── Order Items ──────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS order_items (
    id                   BIGINT        PRIMARY KEY DEFAULT nextval('order_items_id_seq'),
    order_id             BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id           BIGINT        NOT NULL REFERENCES products(id),
    quantity             INTEGER       NOT NULL,
    unit_price           NUMERIC(12,2) NOT NULL,
    subtotal             NUMERIC(14,2) NOT NULL,
    product_name_snapshot VARCHAR(200),
    product_sku_snapshot  VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- ── Notifications ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('notifications_id_seq'),
    user_id         BIGINT       REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(30)  NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    message         TEXT         NOT NULL,
    reference_id    BIGINT,
    reference_type  VARCHAR(50),
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    email_sent      BOOLEAN      NOT NULL DEFAULT FALSE,
    email_sent_at   TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id    ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type       ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_read       ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

-- ── Audit Logs ───────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS audit_logs (
    id               BIGINT        PRIMARY KEY DEFAULT nextval('audit_logs_id_seq'),
    user_id          BIGINT,
    username         VARCHAR(50),
    action           VARCHAR(100)  NOT NULL,
    entity_type      VARCHAR(50),
    entity_id        BIGINT,
    description      TEXT,
    ip_address       VARCHAR(45),
    user_agent       VARCHAR(255),
    success          BOOLEAN       NOT NULL DEFAULT TRUE,
    error_message    VARCHAR(1000),
    execution_time_ms BIGINT,
    old_value        TEXT,
    new_value        TEXT,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_user_id    ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action     ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_entity     ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_success    ON audit_logs(success);
CREATE INDEX IF NOT EXISTS idx_audit_slow       ON audit_logs(execution_time_ms);
