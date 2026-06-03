-- ============================================================
-- Apache Cassandra Schema — Order Service
-- ============================================================
-- This file documents the schema that Spring Data Cassandra
-- creates automatically (schema-action: CREATE_IF_NOT_EXISTS).
--
-- Use this file for:
--   • Manual keyspace/table bootstrap in environments where
--     the application does not have DDL permissions.
--   • Production migration tooling (Cassandra Migrator, etc.)
--   • Reference when switching schema-action to NONE.
-- ============================================================

-- ------------------------------------------------------------
-- Keyspace
-- (SimpleStrategy RF=1 for development; use NetworkTopologyStrategy in prod)
-- ------------------------------------------------------------
CREATE KEYSPACE IF NOT EXISTS order_keyspace
WITH replication = {
    'class'              : 'SimpleStrategy',
    'replication_factor' : 1
}
AND durable_writes = true;

USE order_keyspace;

-- ------------------------------------------------------------
-- User Defined Type: order_item
-- Embedded inside orders and orders_by_user — no join table.
-- ------------------------------------------------------------
CREATE TYPE IF NOT EXISTS order_item (
    food_item_id bigint,
    quantity     int,
    price        decimal
);

-- ------------------------------------------------------------
-- Primary table: orders
-- Partition key  : order_id (UUID) — uniform distribution.
-- Optimistic lock: version column managed by Spring Data @Version → LWT.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    order_id       uuid    PRIMARY KEY,
    user_id        bigint,
    restaurant_id  bigint,
    order_status   text,
    payment_status text,
    total_amount   decimal,
    correlation_id text,
    version        bigint,
    created_at     timestamp,
    updated_at     timestamp,
    items          list<frozen<order_item>>
);

-- ------------------------------------------------------------
-- Materialised-view table: orders_by_user
-- Denormalises order data so all orders for one user are on
-- a single partition.  Written in the same batch as orders.
--
-- Partition key : user_id
-- Clustering    : created_at DESC, order_id ASC
--   → newest orders first, no in-memory sort required.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders_by_user (
    user_id        bigint,
    created_at     timestamp,
    order_id       uuid,
    restaurant_id  bigint,
    order_status   text,
    payment_status text,
    total_amount   decimal,
    correlation_id text,
    items          list<frozen<order_item>>,
    updated_at     timestamp,
    PRIMARY KEY (user_id, created_at, order_id)
) WITH CLUSTERING ORDER BY (created_at DESC, order_id ASC);

-- ------------------------------------------------------------
-- Deduplication table: orders_by_correlation
-- Single partition read to detect duplicate order submissions.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders_by_correlation (
    correlation_id text PRIMARY KEY,
    order_id       uuid
);

-- ------------------------------------------------------------
-- Kafka event state log: order_event_log
-- Append-only — never updated, only inserted.
--
-- Event types  : ORDER_CREATED | ORDER_STATUS_UPDATED |
--                ORDER_CANCELLED | PAYMENT_STATUS_UPDATED
-- Event statuses: PRODUCING → PRODUCED | FAILED → CONSUMED
--
-- Partition key : order_id (all events for one order together)
-- Clustering    : event_id ASC (insertion-time order preserved)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_event_log (
    order_id       uuid,
    event_id       text,
    event_type     text,
    event_status   text,
    correlation_id text,
    payload        text,   -- JSON of the OrderEvent published to Kafka
    error_message  text,   -- non-null only when event_status = FAILED
    created_at     timestamp,
    updated_at     timestamp,
    PRIMARY KEY (order_id, event_id)
) WITH CLUSTERING ORDER BY (event_id ASC);

