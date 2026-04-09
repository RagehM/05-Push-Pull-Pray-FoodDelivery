package com.team05.fooddelivery.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.team05.fooddelivery.order.model.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // CRUD
    @Query("""
            SELECT oi FROM OrderItem oi
            WHERE oi.order.id = :orderId
            """)
    @Transactional(readOnly = true)
    public List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    @Query(value = """
            SELECT name
            FROM menu_items
            WHERE id = :menuItemId
            """,
            nativeQuery = true)
    @Transactional(readOnly = true)
    String getMenuItemName(@Param("menuItemId") Long menuItemId);

    
}