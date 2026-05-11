CREATE SCHEMA IF NOT EXISTS user_svc;

CREATE TABLE IF NOT EXISTS user_svc.user_profiles (
    id           UUID PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    phone        VARCHAR(20),
    address_line VARCHAR(255),
    city         VARCHAR(100),
    updated_at   TIMESTAMP
);
