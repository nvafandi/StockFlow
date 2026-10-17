CREATE TABLE products (
    id          UUID         PRIMARY KEY,
    sku         VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    unit        VARCHAR(30)  NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_products_sku UNIQUE (sku)
);

CREATE INDEX idx_products_name ON products (name);
