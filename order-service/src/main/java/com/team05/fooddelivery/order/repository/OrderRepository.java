package com.team05.fooddelivery.order.repository;

import com.team05.fooddelivery.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = """
            SELECT *
            FROM orders
            WHERE metadata ->> :key = :value
            """, nativeQuery = true)
    List<Order> findByMetadataKeyValue(@Param("key") String key,
                                       @Param("value") String value);
}