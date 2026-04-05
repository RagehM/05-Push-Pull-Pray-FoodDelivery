package com.team05.fooddelivery.restaurant.Repository;

import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.enums.RestaurantStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

//@Repository annotation indicates that this interface is a Spring Data Repository, 
// which will be automatically implemented by Spring Data JPA based on the method signatures defined here.
// The RestaurantRepository interface extends JpaRepository, which provides CRUD operations and pagination for the Restaurant entity.
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    @Query("SELECT r FROM Restaurant r WHERE r.status = :status")
    List<Restaurant> findByStatus(RestaurantStatusEnum status); //S2-F4
    List<Restaurant> findByCuisineType(String cuisineType); //S2-F1
}