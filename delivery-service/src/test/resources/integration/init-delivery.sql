CREATE SCHEMA IF NOT EXISTS delivery;

CREATE TABLE IF NOT EXISTS delivery.delivery_agents (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name    VARCHAR(100) NOT NULL,
    is_free BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS delivery.deliveries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL,
    agent_id    UUID REFERENCES delivery.delivery_agents(id),
    status      VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMP,
    updated_at  TIMESTAMP
);
