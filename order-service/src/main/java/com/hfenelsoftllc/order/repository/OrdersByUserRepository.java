package com.hfenelsoftllc.order.repository;

import com.hfenelsoftllc.order.entity.OrdersByUser;
import com.hfenelsoftllc.order.entity.OrdersByUserKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for the {@code orders_by_user} table.
 *
 * <p>Allows efficient retrieval of all orders belonging to a single user via a
 * partition-key–only query.  Because {@code user_id} is the partition key,
 * this read hits exactly one Cassandra node and scales independently of the
 * total order volume.</p>
 */
@Repository
public interface OrdersByUserRepository extends CassandraRepository<OrdersByUser, OrdersByUserKey> {

    /**
     * Returns all orders for a user, newest first (ORDER BY created_at DESC is
     * baked into the clustering order of {@code orders_by_user}).
     */
    @Query("SELECT * FROM orders_by_user WHERE user_id = ?0 LIMIT ?1")
    List<OrdersByUser> findByUserId(Long userId, int limit);
}

