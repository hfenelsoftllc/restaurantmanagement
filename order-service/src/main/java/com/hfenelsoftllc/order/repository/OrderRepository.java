package com.hfenelsoftllc.order.repository;

import com.hfenelsoftllc.order.entity.Order;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Primary repository for the {@code orders} table.
 *
 * <p>Cassandra does not support ad-hoc filtering on non-primary-key columns without
 * allowing filtering (ALLOW FILTERING) which is expensive.  All alternative access
 * patterns (by userId, by correlationId) are handled through dedicated tables:</p>
 * <ul>
 *   <li>{@link OrdersByUserRepository} — for user-scoped order lists</li>
 *   <li>{@link OrderByCorrelationRepository} — for deduplication checks</li>
 * </ul>
 */
@Repository
public interface OrderRepository extends CassandraRepository<Order, UUID> {
    // findById(UUID) inherited — single-partition read by order_id
}
