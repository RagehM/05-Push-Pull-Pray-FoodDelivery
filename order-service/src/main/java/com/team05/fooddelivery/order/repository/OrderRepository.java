package com.team05.fooddelivery.order.repository;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.dto.OrderAnalyticsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

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
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive);

        // [S3-F2] Check if Restaurant is open
        @Query(value = """
                        SELECT COUNT(*) > 0
                        FROM restaurants r
                        WHERE r.id = :restaurantId
                          AND r.status = 'OPEN'
                        """, nativeQuery = true)
        @Transactional(readOnly = true)
        boolean isRestaurantOpen(@Param("restaurantId") Long restaurantId);

        // [S3-F3] averaging Restaurant's menu price
        @Query(value = """
                        SELECT AVG(menu.price) FROM menu_items menu
                        WHERE menu.restaurant_id = :restaurantId
                        """, nativeQuery = true)
        Double findAverageMenuItemPriceByRestaurantId(@Param("restaurantId") Long restaurantId);

        // [S3-F3] determine surgemultiplayer
        @Query(value = """
                        SELECT COUNT(*) FROM orders ord
                        WHERE ord.restaurant_id = :restaurantId
                            AND ord.status IN ('PLACED', 'CONFIRMED', 'PREPARING')
                        """, nativeQuery = true)
        Long countActiveOrdersByRestaurantId(@Param("restaurantId") Long restaurantId);

        // [S3-F4] Check for create Payment with Pending status
        @Query(value = """
                        INSERT INTO payments (order_id, user_id, amount, method, status, created_at)
                        VALUES (:orderId, :userId, :total, 'CASH_ON_DELIVERY', 'PENDING', NOW())
                        """, nativeQuery = true)
        @Modifying
        @Transactional
        int createPaymentWithPendingStatus(@Param("orderId") Long orderId,
                        @Param("userId") Long userId,
                        @Param("total") Double total);

        // [S3-F5]
        @Query(value = """
                        SELECT *
                        FROM orders
                        WHERE metadata ->> :key = :value
                        """, nativeQuery = true)
        List<Order> findByMetadataKeyValue(@Param("key") String key,
                        @Param("value") String value);

        // [S3-F6] - Order Analytics by Time Period (Report DTO)
        @Query("""
                SELECT
                        COUNT(o),
                        COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN 1L ELSE 0L END), 0L),
                        COALESCE(SUM(CASE WHEN o.status = 'CANCELLED' THEN 1L ELSE 0L END), 0L),
                        COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.totalAmount ELSE 0.0 END), 0.0),
                        COALESCE(AVG(CASE WHEN o.status = 'DELIVERED' THEN o.totalAmount END), 0.0),
                        CASE WHEN COUNT(o) > 0 THEN
                                SUM(CASE WHEN o.status = 'DELIVERED' THEN 1.0 ELSE 0.0 END) * 100.0 / COUNT(o)
                        ELSE 0.0 END
                FROM Order o
                WHERE o.orderDate >= :startDate AND o.orderDate < :endDate
                """)
        @Transactional(readOnly = true)
        Object[][] getOrderAnalyticsByTimePeriod(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // [S3-F7]
        @Transactional
        @Modifying
        @Query(value = """
                        DELETE FROM deliveries
                        where order_id = :orderId AND status = 'ASSIGNED'
                        """, nativeQuery = true)
        void cancelDeliveryByOrderId(@Param("orderId") Long orderId);

        // [S3-F8] Get Order Details with items (Report DTO)
        @Query("""
                        SELECT o FROM Order o
                        LEFT JOIN FETCH o.orderItems
                        WHERE o.id = :orderId
                        """)
        @Transactional(readOnly = true)
        Order getOrderWithOrderItemsById(@Param("orderId") Long orderId);

        // [S3-F9]
        @Query("""
                        SELECT DISTINCT o
                        FROM Order o
                        LEFT JOIN FETCH o.orderItems
                        WHERE o.id = :orderId
                        """)
        Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

        // [S3-F10]
        @Query("""
                        SELECT 
                                COUNT(o),
                                COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.totalAmount ELSE 0.0 END), 0.0),      
                                COALESCE(AVG(CASE WHEN o.status = 'DELIVERED' THEN 1L ELSE 0L END), 0L),
                                COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN 1L ELSE 0L END), 0L),
                                COALESCE(SUM(CASE WHEN o.status = 'CANCELLED' THEN 1L ELSE 0L END), 0L),
                                COALESCE(SUM(CASE WHEN o.status = 'PLACED' THEN 1L ELSE 0L END), 0L),
                                COALESCE(SUM(CASE WHEN o.status = 'CONFIRMED' THEN 1L ELSE 0L END), 0L),
                                COALESCE(SUM(CASE WHEN o.status = 'PREPARING' THEN 1L ELSE 0L END), 0L)

                        FROM Order o
                        WHERE o.orderDate >= :startDate AND o.orderDate < :endDate
                        """)
        @Transactional(readOnly = true)
        Object[][] getOrderCountAndCompletionRateDetails(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // [S3-F11]
        @Query(value = """
                        SELECT u.name FROM users u WHERE u.id = :userId
                        """, nativeQuery = true)
        @Transactional(readOnly = true)
        Optional<String> findUserNameById(@Param("userId") Long userId);

        @Query(value = """
                        SELECT r.name FROM restaurants r WHERE r.id = :restaurantId
                        """, nativeQuery = true)
        @Transactional(readOnly = true)
        Optional<String> findRestaurantNameById(@Param("restaurantId") Long restaurantId);

        @Query(value = """
                        SELECT r.cuisine_type FROM restaurants r WHERE r.id = :restaurantId
                        """, nativeQuery = true)
        @Transactional(readOnly = true)
        Optional<String> findRestaurantCuisineTypeById(@Param("restaurantId") Long restaurantId);


        // [S3-F12] Get Restaurant Recommendations for User (VERIFYING USER IDENTITY)
        @Query(value = """
                        SELECT u.id, u.role FROM users u
                        WHERE u.email = :email
                        """, nativeQuery = true)
        Object[][] verifyUserIsWhoIsMakingRequest(@Param("email") String email);

        @Query(value = "SELECT COUNT(*) > 0 FROM users u WHERE u.id = :userId", nativeQuery = true)
        boolean existsUserById(@Param("userId") Long userId);
        // [CRUD]
        //// Check for existence of user
        @Query(value = """
                        SELECT COUNT(*) > 0 FROM users u
                        WHERE u.id = :userId
                        """, nativeQuery = true)
        @Transactional(readOnly = true)
        boolean existsByUserId(@Param("userId") Long userId);

        //// Check for existence of restaurant
        @Query(value = """
                        SELECT COUNT(*) > 0 FROM restaurants r
                        WHERE r.id = :restaurantId
                        """, nativeQuery = true)
        @Transactional(readOnly = true)
        boolean existsByRestaurantId(@Param("restaurantId") Long restaurantId);

        @Query(value = """
                        SELECT r.id AS restaurantId, r.name AS name, r.cuisine_type AS cuisineType
                        FROM restaurants r
                        WHERE r.id IN (:restaurantIds)
                        """, nativeQuery = true)
        @Transactional(readOnly = true)
        List<RestaurantInfoRow> findRestaurantInfoByIds(@Param("restaurantIds") Collection<Long> restaurantIds);

        interface RestaurantInfoRow {
                Long getRestaurantId();
                String getName();
                String getCuisineType();
        }

        @Query("""
                        SELECT COUNT(o), COALESCE(SUM(o.totalAmount), 0), COALESCE(AVG(o.totalAmount), 0)
                        FROM Order o
                        WHERE o.restaurantId = :restaurantId
                        """)
        @Transactional(readOnly = true)
        Object[] aggregateOrdersByRestaurant(@Param("restaurantId") Long restaurantId);

        @Query("""
                        SELECT COUNT(o)
                        FROM Order o
                        WHERE o.restaurantId = :restaurantId
                          AND o.status IN :activeStatuses
                        """)
        @Transactional(readOnly = true)
        long countActiveOrdersByRestaurant(
                        @Param("restaurantId") Long restaurantId,
                        @Param("activeStatuses") List<OrderStatusEnum> activeStatuses);
}
