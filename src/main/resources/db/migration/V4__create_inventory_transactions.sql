CREATE TABLE inventory_transactions (
    id               UUID         PRIMARY KEY,
    product_id       UUID         NOT NULL REFERENCES products (id),
    warehouse_id     UUID         NOT NULL REFERENCES warehouses (id),
    transaction_type VARCHAR(30)  NOT NULL,
    quantity_delta   BIGINT       NOT NULL,
    quantity_before  BIGINT       NOT NULL,
    quantity_after   BIGINT       NOT NULL,
    reference_id     VARCHAR(100),
    idempotency_key  VARCHAR(150),
    payload_hash     VARCHAR(64),
    reason           VARCHAR(50),
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_inventory_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_inventory_transactions_audit_consistency
        CHECK (quantity_before + quantity_delta = quantity_after)
);

CREATE INDEX idx_inventory_transactions_product   ON inventory_transactions (product_id, created_at);
CREATE INDEX idx_inventory_transactions_warehouse ON inventory_transactions (warehouse_id, created_at);
CREATE INDEX idx_inventory_transactions_reference ON inventory_transactions (reference_id);
