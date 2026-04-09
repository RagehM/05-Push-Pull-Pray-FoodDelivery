package com.team05.fooddelivery.order.repository;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.dto.OrderAnalyticsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

        // [S3-F1]
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

    // [S3-F7]
    @Transactional
    @Modifying
    @Query(value = """
            DELETE FROM deliveries
            where order_id = :orderId AND status = 'ASSIGNED'
            """,
            nativeQuery = true
    )
    void cancelDeliveryByOrderId(@Param("orderId") Long orderId);
  
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

        // [S3-F6] - Order Analytics by Time Period (Report DTO)
        @Query("""
        SELECT new com.team05.fooddelivery.order.dto.OrderAnalyticsDTO(
                COUNT(o),
                COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN o.status = 'CANCELLED' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.totalAmount ELSE 0.0 END), 0.0),
                COALESCE(AVG(CASE WHEN o.status = 'DELIVERED' THEN o.totalAmount END), 0.0),
                CASE WHEN COUNT(o) > 0 THEN
                        SUM(CASE WHEN o.status = 'DELIVERED' THEN 1.0 ELSE 0.0 END) * 100.0 / COUNT(o)
                ELSE 0.0 END
        )
        FROM Order o
        WHERE o.orderDate >= :startDate AND o.orderDate <= :endDate
        """)
        @Transactional(readOnly = true)
        OrderAnalyticsDTO getOrderAnalyticsByTimePeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);



        @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.orderItems
        WHERE o.id = :orderId
        """)
        @Transactional(readOnly = true)
        Order getOrderWithOrderItemsById(@Param("orderId") Long orderId);
}