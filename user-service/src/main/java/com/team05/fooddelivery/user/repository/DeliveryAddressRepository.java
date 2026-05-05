package com.team05.fooddelivery.user.repository;

import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findUserById(@Param("id") Long id);

    List<DeliveryAddress> findAllByUserId(Long userId);

}