package com.team05.fooddelivery.user.repository;

import com.team05.fooddelivery.user.model.User;
import jakarta.transaction.Transactional;
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
      AND (:role IS NULL OR u.user_role = :role)
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


}
