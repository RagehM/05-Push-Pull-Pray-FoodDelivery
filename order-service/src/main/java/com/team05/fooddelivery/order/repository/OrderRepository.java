package com.team05.fooddelivery.order.repository;


import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;
import com.team05.fooddelivery.order.model.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

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

}