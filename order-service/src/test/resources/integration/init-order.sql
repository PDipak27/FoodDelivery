CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE IF NOT EXISTS orders.orders (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   UUID NOT NULL,
    restaurant_id VARCHAR(50) NOT NULL,
    status        VARCHAR(30) NOT NULL,
    total_amount  NUMERIC(10,2) NOT NULL,
    payment_id    UUID,
    delivery_id   UUID,
    created_at    TIMESTAMP DEFAULT now(),
    updated_at    TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS orders.order_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL REFERENCES orders.orders(id),
    item_id     VARCHAR(50) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    quantity    INTEGER NOT NULL,
    unit_price  NUMERIC(10,2) NOT NULL
);
