-- Delivery schema
-- psql -U postgres -d fooddelivery_db -f db-scripts/delivery/V1__init_delivery.sql

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

CREATE INDEX IF NOT EXISTS idx_deliveries_order ON delivery.deliveries(order_id);

-- Seed a few test agents so assign works immediately
INSERT INTO delivery.delivery_agents (user_id, name, is_free) VALUES
    (gen_random_uuid(), 'Agent Alpha', true),
    (gen_random_uuid(), 'Agent Beta',  true),
    (gen_random_uuid(), 'Agent Gamma', true);
