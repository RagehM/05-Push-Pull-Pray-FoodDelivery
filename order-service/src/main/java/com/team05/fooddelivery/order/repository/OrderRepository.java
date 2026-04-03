package com.team05.fooddelivery.order.repository;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        SELECT o
        FROM Order o
        WHERE o.orderDate >= :startDateTime
          AND o.orderDate < :endDateTimeExclusive
          AND (:status IS NULL OR o.status = :status)
        ORDER BY o.orderDate DESC
    """)
    List<Order> searchByStatusAndDateRange(
            @Param("status") OrderStatusEnum status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive
    );
}