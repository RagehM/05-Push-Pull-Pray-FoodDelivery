package com.team05.fooddelivery.restaurant.Repository;

import com.team05.fooddelivery.restaurant.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// @Repository annotation indicates that this interface is a Spring Data Repository,
// which will be automatically implemented by Spring Data JPA based on the method signatures defined here.
// The MenuItemRepository interface extends JpaRepository, which provides CRUD operations and pagination for the MenuItem entity.

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantId(Long restaurantId); 
    List<MenuItem> findByRestaurantIdAndAvailable(Long restaurantId, Boolean available);
}