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

        List<Restaurant> findByStatus(RestaurantStatusEnum status);

        List<Restaurant> findByCuisineType(CuisineTypeEnum cuisineType);


        // [S2-F1] Filters by optional cuisine type and rating range, ordered by rating
        // descending.
        @Query(value = "SELECT * FROM restaurants WHERE " +
                        "(:cuisineType IS NULL OR cuisine_type = :cuisineType) " +
                        "AND rating BETWEEN :minRating AND :maxRating " +
                        "ORDER BY rating DESC", nativeQuery = true)
        List<Restaurant> searchByCuisineAndRating(
                        @Param("cuisineType") String cuisineType,
                        @Param("minRating") Double minRating,
                        @Param("maxRating") Double maxRating);

        // [S2-F5] Filters restaurants by a JSONB details key-value pair with an
        // optional status constraint.
        @Query(value = """
                            SELECT * FROM restaurants
                            WHERE details ->> :key = :value
                            AND (:status IS NULL OR status = :status)
                        """, nativeQuery = true)
        List<Restaurant> findByDetailAttribute(
                        @Param("key") String key,
                        @Param("value") String value,
                        @Param("status") String status);

        // [S2-F6] Joins restaurants with orders to compute total order count, ordered
        // by rating descending with a row limit.
        @Query(value = """
                        SELECT r.id, r.name, r.rating,
                               COUNT(o.id) as total_orders
                        FROM restaurants r
                        LEFT JOIN orders o ON o.restaurant_id = r.id
                        GROUP BY r.id, r.name, r.rating
                        ORDER BY r.rating DESC
                        LIMIT :limit
                        """, nativeQuery = true)
        List<Object[]> findTopRatedRestaurants(@Param("limit") int limit);

        // [S2-F8] Checks whether a given user ID belongs to an ADMIN user, used to
        // authorize menu item toggling.
        @Query(value = "SELECT COUNT(*) FROM users WHERE id = :userId AND role = 'ADMIN'", nativeQuery = true)
        int countAdminById(@Param("userId") Long userId);

        // [S2-F9] Returns distinct restaurants that have at least one unavailable menu
        // item.
        @Query(value = "SELECT DISTINCT r.* FROM restaurants r JOIN menu_items m ON m.restaurant_id = r.id WHERE m.available = false", nativeQuery = true)
        List<Restaurant> findRestaurantsWithUnavailableItems();

        // [S2-F12] Counts active (available) menu items for a restaurant
        @Query(value = "SELECT COUNT(*) FROM menu_items WHERE restaurant_id = :restaurantId AND available = true", nativeQuery = true)
        Long countActiveMenuItems(@Param("restaurantId") Long restaurantId);

}