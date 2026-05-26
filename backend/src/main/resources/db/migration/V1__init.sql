-- V1__init.sql

-- ─── Categories ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_categories_name UNIQUE (name)
);

-- ─── Products ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)   NOT NULL,
    price       NUMERIC(19, 2) NOT NULL,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT uq_products_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);

-- ─── Carts ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS carts (
    id         BIGSERIAL    PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_carts_session_id UNIQUE (session_id)
);

-- ─── Cart Items ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT    NOT NULL REFERENCES carts(id)    ON DELETE CASCADE,
    product_id BIGINT    NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity   INT       NOT NULL CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_items(cart_id);

-- ─── Orders ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id                   BIGSERIAL      PRIMARY KEY,
    created_at           TIMESTAMP      NOT NULL,
    total_price          NUMERIC(19, 2) NOT NULL,
    session_id           VARCHAR(255),
    customer_email       VARCHAR(255),
    customer_phone       VARCHAR(50),
    delivery_address     VARCHAR(512),
    yookassa_payment_id  VARCHAR(255),
    delivery_type        VARCHAR(50)    NOT NULL DEFAULT 'CDEK',
    payment_method       VARCHAR(50)    NOT NULL DEFAULT 'SBP',
    payment_status       VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    status               VARCHAR(50)    NOT NULL DEFAULT 'NEW',
    track_number         VARCHAR(255),
    receipt_number       VARCHAR(255),
    paid_at              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_session_id     ON orders(session_id);
CREATE INDEX IF NOT EXISTS idx_orders_payment_status ON orders(payment_status);
CREATE INDEX IF NOT EXISTS idx_orders_status         ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at     ON orders(created_at);

-- ─── Order Items ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id           BIGSERIAL      PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_name VARCHAR(255)   NOT NULL,
    price        NUMERIC(19, 2) NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);

-- ─── Initial Data ─────────────────────────────────────────────────────────────
INSERT INTO categories (name) VALUES
    ('Груза на литые диски (алюминиевые)'),
    ('Груза на штампованные диски'),
    ('Самоклеящиеся свинцовые груза'),
    ('Самоклеящиеся груза Zn')
ON CONFLICT (name) DO NOTHING;