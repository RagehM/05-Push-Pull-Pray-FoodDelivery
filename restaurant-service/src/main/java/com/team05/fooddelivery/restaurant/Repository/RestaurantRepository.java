package com.team05.fooddelivery.restaurant.repository;

import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.enums.RestaurantStatusEnum;
import com.team05.fooddelivery.restaurant.enums.CuisineTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    @Query(value = "SELECT * FROM restaurants WHERE status = CAST(:status AS restaurantstatusenum)", nativeQuery = true)
    List<Restaurant> findByStatus(@Param("status") String status);

    List<Restaurant> findByCuisineType(CuisineTypeEnum cuisineType);

//S2-F1
    @Query(value = "SELECT * FROM restaurants WHERE " +
       "(:cuisineType IS NULL OR cuisine_type = :cuisineType) " +
       "AND rating BETWEEN :minRating AND :maxRating " +
       "ORDER BY rating DESC", nativeQuery = true)
   List<Restaurant> searchByCuisineAndRating(
    @Param("cuisineType") String cuisineType,
    @Param("minRating") Double minRating,
    @Param("maxRating") Double maxRating);
}