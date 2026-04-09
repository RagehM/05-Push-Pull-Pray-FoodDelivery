package com.team05.fooddelivery.order.repository;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Repository
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

        // [CRUD]
        //// Check for existence of user
        @Query(value =  """
                        SELECT COUNT(*) > 0 FROM users u 
                        WHERE u.id = :userId
                        """, 
                nativeQuery = true)
        @Transactional(readOnly = true)
        boolean existsByUserId(@Param("userId") Long userId);
        //// Check for existence of restaurant
        @Query(value =  """
                        SELECT COUNT(*) > 0 FROM restaurants r 
                        WHERE r.id = :restaurantId
                        """, 
                nativeQuery = true)
        @Transactional(readOnly = true)
        boolean existsByRestaurantId(@Param("restaurantId") Long restaurantId);
}