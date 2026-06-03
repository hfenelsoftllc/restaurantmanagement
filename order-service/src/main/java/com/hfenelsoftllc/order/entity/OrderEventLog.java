package com.hfenelsoftllc.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

/**
 * Cassandra event-sourcing log table: {@code order_event_log}.
 *
 * <p>Every state change in the order lifecycle — and every Kafka produce/consume
 * action — is appended here as an immutable event row.  This provides:</p>
 * <ul>
 *   <li><b>Full audit trail</b> — who changed what, when, and why.</li>
 *   <li><b>Kafka delivery tracking</b> — distinguish PRODUCING → PRODUCED vs FAILED.</li>
 *   <li><b>Consumer acknowledgement</b> — payment services write CONSUMED records.</li>
 *   <li><b>Replay support</b> — re-read the log to reconstruct order state.</li>
 * </ul>
 *
 * <h3>Event Types</h3>
 * <ul>
 *   <li>{@code ORDER_CREATED}          — order first persisted</li>
 *   <li>{@code ORDER_STATUS_UPDATED}   — order status transition</li>
 *   <li>{@code ORDER_CANCELLED}        — order cancelled</li>
 *   <li>{@code PAYMENT_STATUS_UPDATED} — payment status changed (by payment consumer)</li>
 * </ul>
 *
 * <h3>Event Statuses</h3>
 * <ul>
 *   <li>{@code PRODUCING} — event is about to be sent to Kafka</li>
 *   <li>{@code PRODUCED}  — Kafka confirmed delivery (acks=all)</li>
 *   <li>{@code FAILED}    — Kafka publish failed (see errorMessage)</li>
 *   <li>{@code CONSUMED}  — downstream consumer acknowledged processing</li>
 * </ul>
 *
 * <p>CQL equivalent:
 * <pre>
 * CREATE TABLE order_event_log (
 *   order_id      uuid,
 *   event_id      text,
 *   event_type    text,
 *   event_status  text,
 *   correlation_id text,
 *   payload       text,
 *   error_message text,
 *   created_at    timestamp,
 *   updated_at    timestamp,
 *   PRIMARY KEY (order_id, event_id)
 * ) WITH CLUSTERING ORDER BY (event_id ASC);
 * </pre>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("order_event_log")
public class OrderEventLog {

    @PrimaryKey
    private OrderEventLogKey key;

    /** e.g. ORDER_CREATED, ORDER_STATUS_UPDATED, ORDER_CANCELLED, PAYMENT_STATUS_UPDATED */
    @Column("event_type")
    private String eventType;

    /** e.g. PRODUCING, PRODUCED, FAILED, CONSUMED */
    @Column("event_status")
    private String eventStatus;

    @Column("correlation_id")
    private String correlationId;

    /** JSON serialisation of the {@link com.hfenelsoftllc.order.dto.OrderEvent} payload. */
    @Column("payload")
    private String payload;

    /** Non-null only when eventStatus = FAILED. */
    @Column("error_message")
    private String errorMessage;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}

