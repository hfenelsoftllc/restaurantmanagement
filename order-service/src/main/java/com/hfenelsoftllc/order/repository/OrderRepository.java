package com.hfenelsoftllc.order.repository;

import com.hfenelsoftllc.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderIdAndUserId(Long orderId, Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    boolean existsByCorrelationId(String correlationId);

    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.userId = :userId")
    Optional<Order> findForUser(@Param("orderId") Long orderId, @Param("userId") Long userId);
}

