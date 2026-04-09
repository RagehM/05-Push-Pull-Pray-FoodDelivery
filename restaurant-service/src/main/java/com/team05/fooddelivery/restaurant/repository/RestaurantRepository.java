package com.team05.fooddelivery.restaurant.repository;

import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.enums.RestaurantStatusEnum;
import com.team05.fooddelivery.restaurant.enums.CuisineTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByStatus(RestaurantStatusEnum status);

    List<Restaurant> findByCuisineType(CuisineTypeEnum cuisineType);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE restaurant_id = :id AND status IN ('CONFIRMED', 'PREPARING')", nativeQuery = true)
    int countActiveOrders(@Param("id") Long restaurantId);

    // S2-F1: Implement search functionality in RestaurantRepository to allow
    // filtering by cuisine type and rating range.
    // S2-F1
    @Query(value = "SELECT * FROM restaurants WHERE " +
            "(:cuisineType IS NULL OR cuisine_type = :cuisineType) " +
            "AND rating BETWEEN :minRating AND :maxRating " +
            "ORDER BY rating DESC", nativeQuery = true)
    List<Restaurant> searchByCuisineAndRating(
            @Param("cuisineType") String cuisineType,
            @Param("minRating") Double minRating,
            @Param("maxRating") Double maxRating);

    // S2-F3
    @Query(value = "SELECT COUNT(o.id), COALESCE(SUM(o.total_amount), 0), COALESCE(AVG(o.total_amount), 0) " +
            "FROM orders o WHERE o.restaurant_id = :restaurantId " +
            "AND o.status = 'DELIVERED' " +
            "AND o.order_date BETWEEN CAST(:startDate AS timestamp) AND CAST(:endDate AS timestamp)", nativeQuery = true)
    List<Object[]> getRevenueSummary(
            @Param("restaurantId") Long restaurantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
//S2-F5
    @Query(value = """
        SELECT * FROM restaurants
        WHERE details ->> :key = :value
        AND (:status IS NULL OR status = :status)
    """, nativeQuery = true)
    List<Restaurant> findByDetailAttribute(
            @Param("key") String key,
            @Param("value") String value,
            @Param("status") String status
    );
}