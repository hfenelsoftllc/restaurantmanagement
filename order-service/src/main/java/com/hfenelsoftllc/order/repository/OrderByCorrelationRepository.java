package com.hfenelsoftllc.order.repository;

import com.hfenelsoftllc.order.entity.OrderByCorrelation;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the {@code orders_by_correlation} deduplication table.
 *
 * <p>Before creating an order, the service calls {@link #existsById(Object)}
 * to detect duplicate submissions carrying the same {@code correlationId}.
 * Because {@code correlation_id} is the sole partition key, this is a
 * constant-time read regardless of total order volume.</p>
 */
@Repository
public interface OrderByCorrelationRepository extends CassandraRepository<OrderByCorrelation, String> {
    // existsById(correlationId) and findById(correlationId) are inherited
}

