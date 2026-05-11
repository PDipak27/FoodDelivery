CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE IF NOT EXISTS payments.payments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID NOT NULL,
    amount     NUMERIC(10,2) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);
