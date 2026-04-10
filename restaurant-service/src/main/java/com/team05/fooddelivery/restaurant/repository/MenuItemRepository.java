package com.team05.fooddelivery.restaurant.repository;

import com.team05.fooddelivery.restaurant.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
// @Repository annotation indicates that this interface is a Spring Data Repository,
// which will be automatically implemented by Spring Data JPA based on the method signatures defined here.
// The MenuItemRepository interface extends JpaRepository, which provides CRUD operations and pagination for the MenuItem entity.

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
  List<MenuItem> findByRestaurantId(Long restaurantId);

  List<MenuItem> findByRestaurantIdAndAvailable(Long restaurantId, Boolean available);

  // [S2-F8] Counts pending order items referencing a menu item, used to block toggling it to unavailable.
  @Query(value = "SELECT COUNT(*) FROM order_items WHERE menu_item_id = :menuItemId AND status = 'PENDING'", nativeQuery = true)
  int countPendingOrderItems(@Param("menuItemId") Long menuItemId);
}
