package com.hfenelsoftllc.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Primary Cassandra table: {@code orders}.
 *
 * <p>Partition key is {@code order_id} (UUID) — every order lookup by ID is a single
 * partition read.  Order items are embedded as a list of the {@link OrderItem} UDT,
 * completely eliminating the {@code order_items} join table and the
 * {@code @ManyToOne} / {@code @OneToMany} relationship complexity.</p>
 *
 * <p>Optimistic locking is enforced via {@link Version} which maps to a Cassandra
 * Lightweight Transaction ({@code UPDATE … IF version = ?}).</p>
 *
 * <p>Spring Data Cassandra automatically maps Java types:
 * UUID→uuid, Long→bigint, BigDecimal→decimal, Instant→timestamp,
 * List&lt;OrderItem&gt;→list&lt;frozen&lt;order_item&gt;&gt;.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    /** UUID-based primary key — distributes uniformly across Cassandra nodes. */
    @Id
    @Column("order_id")
    private UUID orderId;

    @Column("user_id")
    private Long userId;

    @Column("restaurant_id")
    private Long restaurantId;

    /** Lifecycle status: PENDING | PAID | CANCELLED */
    @Column("order_status")
    private String orderStatus;

    /** Payment status: UNPAID | PROCESSING | PAID | FAILED */
    @Column("payment_status")
    private String paymentStatus;

    @Column("total_amount")
    private BigDecimal totalAmount;

    /** UUID string for Kafka deduplication and end-to-end audit trail. */
    @Column("correlation_id")
    private String correlationId;

    /**
     * Optimistic-lock version — Spring Data Cassandra translates saves to
     * {@code UPDATE … IF version = :expected} (Paxos LWT).
     */
    @Version
    @Column("version")
    private Long version;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    /**
     * Embedded line items stored as a {@code list<frozen<order_item>>}.
     * No separate table, no foreign key, no join required.
     */
    @Column("items")
    private List<OrderItem> items;
}
