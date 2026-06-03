package com.hfenelsoftllc.order.repository;

import com.hfenelsoftllc.order.entity.OrderEventLog;
import com.hfenelsoftllc.order.entity.OrderEventLogKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for the {@code order_event_log} table.
 *
 * <p>All Kafka event state transitions (PRODUCING → PRODUCED / FAILED → CONSUMED)
 * are written here.  Every row is immutable — only new rows are inserted, never
 * updated, to maintain a true append-only event log.</p>
 */
@Repository
public interface OrderEventLogRepository extends CassandraRepository<OrderEventLog, OrderEventLogKey> {

    /**
     * Returns the full event timeline for a given order, sorted oldest-first
     * (baked into the clustering order of the table).
     */
    @Query("SELECT * FROM order_event_log WHERE order_id = ?0")
    List<OrderEventLog> findByOrderId(UUID orderId);
}

