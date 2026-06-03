package com.hfenelsoftllc.order.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for {@link OrderEventLog}.
 *
 * <ul>
 *   <li>{@code order_id} — partition key: all events for one order land together.</li>
 *   <li>{@code event_id} — clustering key (ASC): preserves event insertion order
 *       for efficient timeline reads.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyClass
public class OrderEventLogKey implements Serializable {

    @PrimaryKeyColumn(name = "order_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID orderId;

    @PrimaryKeyColumn(name = "event_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED,
                      ordering = Ordering.ASCENDING)
    private String eventId;
}

