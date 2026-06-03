package com.hfenelsoftllc.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Cassandra materialized-view table: {@code orders_by_user}.
 *
 * <p>Denormalises all order data so that fetching a user's order history requires
 * only a single partition read (no scatter-gather across the {@code orders} table).
 * Written in the same batch as {@link Order} to maintain consistency.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders_by_user")
public class OrdersByUser {

    @PrimaryKey
    private OrdersByUserKey key;

    @Column("restaurant_id")
    private Long restaurantId;

    @Column("order_status")
    private String orderStatus;

    @Column("payment_status")
    private String paymentStatus;

    @Column("total_amount")
    private BigDecimal totalAmount;

    @Column("correlation_id")
    private String correlationId;

    /** Embedded items — same UDT as in the primary {@code orders} table. */
    @Column("items")
    private List<OrderItem> items;

    @Column("updated_at")
    private Instant updatedAt;
}
