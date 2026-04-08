package com.team05.fooddelivery.order.repository;

import com.team05.fooddelivery.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

        @Query("""
           SELECT DISTINCT o
           FROM Order o
           LEFT JOIN FETCH o.orderItems
           WHERE o.id = :orderId
           """)
        Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

        @Query(value = """
                   SELECT COUNT(*) > 0
                   FROM users u
                   WHERE u.id = :userId
                   """, nativeQuery = true)
        @Transactional(readOnly = true)
        boolean existsByUserId(@Param("userId") Long userId);

        @Query(value = """
                   SELECT COUNT(*) > 0
                   FROM restaurants r
                   WHERE r.id = :restaurantId
                   """, nativeQuery = true)
        @Transactional(readOnly = true)
        boolean existsByRestaurantId(@Param("restaurantId") Long restaurantId);
}