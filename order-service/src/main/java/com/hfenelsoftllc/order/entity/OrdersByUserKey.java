package com.hfenelsoftllc.order.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Composite primary key for the {@code orders_by_user} materialized-view table.
 *
 * <p>CQL key definition:
 * <pre>
 * PRIMARY KEY (user_id, created_at, order_id)
 * WITH CLUSTERING ORDER BY (created_at DESC, order_id ASC)
 * </pre>
 * </p>
 *
 * <ul>
 *   <li>{@code user_id} — partition key, ensures all orders for one user land on the
 *       same Cassandra node (predictable read performance).</li>
 *   <li>{@code created_at} DESC — clustering key, keeps newest orders first without
 *       in-memory sorting.</li>
 *   <li>{@code order_id} — uniqueness within the same timestamp bucket.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyClass
public class OrdersByUserKey implements Serializable {

    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long userId;

    @PrimaryKeyColumn(name = "created_at", ordinal = 1, type = PrimaryKeyType.CLUSTERED,
                      ordering = Ordering.DESCENDING)
    private Instant createdAt;

    @PrimaryKeyColumn(name = "order_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED,
                      ordering = Ordering.ASCENDING)
    private UUID orderId;
}

