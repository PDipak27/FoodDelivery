-- Run this first against fooddelivery_db.
-- Creates all schemas used by the services.
-- psql -U postgres -d fooddelivery_db -f db-scripts/00_create_schemas.sql

-- Note: CREATE DATABASE cannot run inside a transaction/script connected to the DB.
-- Create the DB separately: psql -U postgres -c "CREATE DATABASE fooddelivery_db;"

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS user_svc;
CREATE SCHEMA IF NOT EXISTS orders;
CREATE SCHEMA IF NOT EXISTS payments;
CREATE SCHEMA IF NOT EXISTS delivery;
