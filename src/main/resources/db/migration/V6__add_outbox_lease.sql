-- Lease untuk klaim event oleh outbox publisher (FSD §10.3 lock-lease).
-- NULL = event tidak sedang dipegang publisher mana pun.
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS leased_at TIMESTAMPTZ;
