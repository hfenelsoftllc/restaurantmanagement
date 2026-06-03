package com.hfenelsoftllc.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

/**
 * Cassandra lookup table: {@code orders_by_correlation}.
 *
 * <p>Used exclusively for idempotency / deduplication checks.  Before an order is
 * created the service queries this table to verify the {@code correlationId} has not
 * been seen before.  Because the table has a single-column primary key the check is
 * a constant-time partition read.</p>
 *
 * <p>CQL equivalent:
 * <pre>
 * CREATE TABLE orders_by_correlation (
 *   correlation_id text PRIMARY KEY,
 *   order_id       uuid
 * );
 * </pre>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders_by_correlation")
public class OrderByCorrelation {

    @Id
    @Column("correlation_id")
    private String correlationId;

    @Column("order_id")
    private UUID orderId;
}

