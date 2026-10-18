CREATE TABLE outbox_events (
    id             UUID         PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(150) NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    published_at   TIMESTAMPTZ,
    attempts       INTEGER      NOT NULL DEFAULT 0,
    last_error     TEXT
);

CREATE INDEX idx_outbox_created_at ON outbox_events (created_at);
CREATE INDEX idx_outbox_pending    ON outbox_events (created_at) WHERE published_at IS NULL;
