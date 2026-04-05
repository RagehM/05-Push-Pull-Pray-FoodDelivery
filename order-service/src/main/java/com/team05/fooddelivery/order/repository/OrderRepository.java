package com.team05.fooddelivery.order.repository;

import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;
import com.team05.fooddelivery.order.model.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


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
}