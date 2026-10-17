CREATE TABLE warehouses (
    id         UUID         PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    location   TEXT,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_warehouses_code UNIQUE (code)
);
