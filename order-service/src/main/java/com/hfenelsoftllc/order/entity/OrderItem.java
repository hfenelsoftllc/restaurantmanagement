package com.hfenelsoftllc.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.math.BigDecimal;

/**
 * Cassandra User Defined Type (UDT) for order line items.
 *
 * <p>Stored as a {@code frozen<order_item>} inside the {@code items} column of the
 * {@code orders} and {@code orders_by_user} tables.  Because items are embedded in
 * the parent row, there is no separate table, no join, and no foreign key — this
 * directly eliminates the relational complexity that existed with the JPA
 * {@code @ManyToOne} / {@code @OneToMany} mapping.</p>
 *
 * <p>Spring Data Cassandra automatically maps: Long→BIGINT, Integer→INT, BigDecimal→DECIMAL.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@UserDefinedType("order_item")
public class OrderItem {

    private Long foodItemId;
    private Integer quantity;
    private BigDecimal price;
}
