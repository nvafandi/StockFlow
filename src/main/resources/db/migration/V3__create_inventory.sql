CREATE TABLE inventory (
    id           UUID        PRIMARY KEY,
    product_id   UUID        NOT NULL REFERENCES products (id),
    warehouse_id UUID        NOT NULL REFERENCES warehouses (id),
    quantity     BIGINT      NOT NULL DEFAULT 0,
    version      BIGINT      NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_inventory_product_warehouse UNIQUE (product_id, warehouse_id),
    CONSTRAINT ck_inventory_quantity_non_negative CHECK (quantity >= 0)
);
