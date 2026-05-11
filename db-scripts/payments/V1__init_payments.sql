-- Payments schema
-- psql -U postgres -d fooddelivery_db -f db-scripts/payments/V1__init_payments.sql

CREATE TABLE IF NOT EXISTS payments.payments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID NOT NULL,
    amount     NUMERIC(10,2) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payments_order ON payments.payments(order_id);
