package com.team05.fooddelivery.user.repository;

import com.team05.fooddelivery.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = """
    SELECT *
    FROM users u
    WHERE (:name IS NULL OR u.name ILIKE '%' || :name || '%')
      AND (:email IS NULL OR u.email ILIKE '%' || :email || '%')
      AND (:role IS NULL OR u.role = :role)
    """, nativeQuery = true)
    List<User> searchUsers(
            @Param("name") String name,
            @Param("email") String email,
            @Param("role") String role
    );


    @Query(value = """
    SELECT * FROM orders o 
    WHERE o.user_id = :userId 
    AND o.status IN ('PLACED', 'PREPARING', 'CONFIRMED')
    """,
            nativeQuery = true)
    List<Object> findOrdersByUserId(@Param("userId") Long userId);

 
    @Query(           
            value = """
    SELECT * FROM users u WHERE u.preferences ->> ?1 = ?2

    """,
            nativeQuery = true
    )
    List<User> findUserByPreferencesContaining(String key, String value );


    @Query(value = """
    SELECT o.id, o.user_id, o.restaurant_id, o.delivery_address, o.status, o.total_amount, o.metadata, o.order_date, o.delivered_at
    FROM orders o 
    WHERE o.user_id = :userId
    """,
            nativeQuery = true)
    List<Object[]> findTotalOrders(@Param("userId") Long userId);


    @Query(value = """
    SELECT o.id, o.user_id, o.restaurant_id, o.delivery_address, o.status, o.total_amount, o.metadata, o.order_date, o.delivered_at
    FROM orders o 
    WHERE o.user_id = :userId 
    AND o.status = 'DELIVERED'
    """,
            nativeQuery = true)
    List<Object[]> findDeliveredOrders(@Param("userId") Long userId);

    @Query(value = """
    SELECT o.id, o.user_id, o.restaurant_id, o.delivery_address, o.status, o.total_amount, o.metadata, o.order_date, o.delivered_at
    FROM orders o 
    WHERE o.user_id = :userId 
    AND o.status = 'CANCELLED'
    """,
            nativeQuery = true)
    List<Object[]> findCancelledOrders(@Param("userId") Long userId);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
