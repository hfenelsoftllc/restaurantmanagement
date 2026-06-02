-- ============================================================
-- Order Service Database Schema
-- ============================================================
-- Run this script once against MySQL after the DB container starts.
-- docker exec -i mysql mysql -uroot -p${MYSQL_PASSWORD} < schema/order-service-schema.sql

CREATE DATABASE IF NOT EXISTS order_service_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE order_service_db;

-- ============================================================
-- Orders Table (core entity)
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    order_id       BIGINT         NOT NULL AUTO_INCREMENT,
    user_id        BIGINT         NOT NULL,
    restaurant_id  BIGINT         NOT NULL,
    order_status   VARCHAR(50)    NOT NULL DEFAULT 'PENDING'  COMMENT 'PENDING | PAID | CANCELLED',
    payment_status VARCHAR(50)    NOT NULL DEFAULT 'UNPAID'   COMMENT 'UNPAID | PROCESSING | PAID | FAILED',
    total_amount   DECIMAL(10,2)  NOT NULL,
    correlation_id VARCHAR(255)   NOT NULL UNIQUE             COMMENT 'UUID for idempotency & audit trail',
    version        BIGINT         NOT NULL DEFAULT 0          COMMENT 'Optimistic locking',
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (order_id),
    INDEX idx_orders_user_id        (user_id),
    INDEX idx_orders_restaurant_id  (restaurant_id),
    INDEX idx_orders_correlation_id (correlation_id),
    INDEX idx_orders_status         (order_status),
    INDEX idx_orders_payment_status (payment_status),
    INDEX idx_orders_created_at     (created_at),
    INDEX idx_orders_user_restaurant (user_id, restaurant_id),
    INDEX idx_orders_status_ts      (order_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Order Items Table
-- ============================================================
CREATE TABLE IF NOT EXISTS order_items (
    item_id      BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    food_item_id BIGINT        NOT NULL,
    quantity     INT           NOT NULL,
    price        DECIMAL(10,2) NOT NULL,

    PRIMARY KEY (item_id),
    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_food_id  (food_item_id),
    INDEX idx_order_items_both     (order_id, food_item_id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Order Events Audit Table (immutable audit log)
-- ============================================================
CREATE TABLE IF NOT EXISTS order_events (
    event_id   BIGINT       NOT NULL AUTO_INCREMENT,
    order_id   BIGINT       NOT NULL,
    event_type VARCHAR(100) NOT NULL COMMENT 'ORDER_CREATED | ORDER_CANCELLED | PAYMENT_INITIATED | PAYMENT_COMPLETED | PAYMENT_FAILED',
    payload    JSON                  COMMENT 'Full event payload for replay',
    signature  VARCHAR(500)          COMMENT 'HMAC-SHA256 base64url-encoded signature',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (event_id),
    INDEX idx_order_events_order_id  (order_id),
    INDEX idx_order_events_type      (event_type),
    INDEX idx_order_events_created   (created_at),
    CONSTRAINT fk_order_events_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Payment Results Table (written by payment consumers)
-- ============================================================
CREATE TABLE IF NOT EXISTS payment_results (
    result_id       BIGINT       NOT NULL AUTO_INCREMENT,
    order_id        BIGINT       NOT NULL,
    correlation_id  VARCHAR(255) NOT NULL,
    provider        VARCHAR(50)  NOT NULL COMMENT 'INTERNAL | STRIPE | PAYPAL',
    status          VARCHAR(50)  NOT NULL COMMENT 'SUCCESS | FAILED | PENDING',
    provider_ref    VARCHAR(255)          COMMENT 'External transaction ID',
    amount          DECIMAL(10,2)         NOT NULL,
    error_message   TEXT,
    processed_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (result_id),
    INDEX idx_payment_results_order_id       (order_id),
    INDEX idx_payment_results_correlation_id (correlation_id),
    INDEX idx_payment_results_status         (status),
    CONSTRAINT fk_payment_results_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

