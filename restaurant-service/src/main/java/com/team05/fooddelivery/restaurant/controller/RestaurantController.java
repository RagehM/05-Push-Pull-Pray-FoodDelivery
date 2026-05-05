package com.team05.fooddelivery.restaurant.controller;

import com.team05.fooddelivery.restaurant.dto.RestaurantRevenueDTO;
import com.team05.fooddelivery.restaurant.dto.TopRestaurantDTO;
import com.team05.fooddelivery.restaurant.dto.RestaurantDashboardDTO;
import com.team05.fooddelivery.restaurant.dto.RestaurantMenuAlertDTO;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.service.RestaurantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.team05.fooddelivery.restaurant.service.MenuItemService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.team05.fooddelivery.restaurant.dto.RestaurantMenuAlertDTO;
import com.team05.fooddelivery.restaurant.model.elasticsearch.RestaurantSearchDocument;
@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    // The RestaurantController class is a REST controller that handles HTTP requests related to restaurant operations.
    // It uses the RestaurantService to perform business logic and interact with the database.
    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;

    public RestaurantController(RestaurantService restaurantService, MenuItemService menuItemService) {
        this.restaurantService = restaurantService;
        this.menuItemService = menuItemService;
    }

    // The create method handles POST requests to create a new restaurant.
    @PostMapping
    public ResponseEntity<Restaurant> create(@RequestBody Restaurant restaurant) {
        return ResponseEntity.ok(restaurantService.create(restaurant));
    }

    // The getById method handles GET requests to retrieve a restaurant by its ID.
    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getById(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getById(id));
    }

    // The getAll method handles GET requests to retrieve all restaurants.
    @GetMapping
    public ResponseEntity<List<Restaurant>> getAll() {
        return ResponseEntity.ok(restaurantService.getAll());
    }

    // The update method handles PUT requests to update an existing restaurant.
    @PutMapping("/{id}")
    public ResponseEntity<Restaurant> update(@PathVariable Long id, @RequestBody Restaurant restaurant) {
        return ResponseEntity.ok(restaurantService.update(id, restaurant));
    }

    // The delete method handles DELETE requests to remove a restaurant by its ID.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        restaurantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // [S2-F1] Search Restaurants by Cuisine and Rating Range
    @GetMapping("/search")
    public ResponseEntity<List<Restaurant>> search(
            @RequestParam(required = false) String cuisineType,
            @RequestParam Double minRating,
            @RequestParam Double maxRating) {
        return ResponseEntity.ok(restaurantService.searchByCuisineAndRating(cuisineType, minRating, maxRating));
    }

    // [S2-F2] Update Restaurant Details (JSONB Partial Update)
    @PutMapping("/{id}/details")
    public ResponseEntity<Restaurant> updateDetails(@PathVariable Long id, @RequestBody Map<String, Object> details) {
        return ResponseEntity.ok(restaurantService.updateDetails(id, details));
    }

    // [S2-F3] Get Restaurant Order Revenue Summary
    @GetMapping("/{id}/revenue")
    public ResponseEntity<RestaurantRevenueDTO> getRevenueSummary(
            @PathVariable Long id,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
        return ResponseEntity.ok(restaurantService.getRevenueSummary(id, start, end));
    }

    // [S2-F4] Update Restaurant Status (Transactional)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        restaurantService.updateRestaurantStatus(id, status);
        return ResponseEntity.ok().build();
    }

    // [S2-F5] Filter Restaurants by Detail Attribute (JSONB)
    @GetMapping("/details/search")
    public ResponseEntity<List<Restaurant>> filterByDetail(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(restaurantService.filterByDetail(key, value, status));
    }

    // [S2-F6] Top Rated Restaurants Report
    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopRestaurantDTO>> getTopRated(
            @RequestParam int limit) {
        return ResponseEntity.ok(restaurantService.getTopRated(limit));
    }

    // [S2-F7] Rate a Restaurant After Order (Transactional)
    @PostMapping("/{id}/rate")
    public ResponseEntity<Void> rateRestaurant(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        Integer rating = Integer.valueOf(body.get("rating").toString());
        restaurantService.rateRestaurant(id, orderId, rating);
        return ResponseEntity.ok().build();
    }

    // [S2-F8] Toggle Menu Item Availability (Transactional)
    @PutMapping("/{restaurantId}/menu-items/{menuItemId}/toggle")
    public ResponseEntity<Restaurant> toggleMenuItemAvailability(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId,
            @RequestBody Map<String, Object> body) {
        Long toggledBy = Long.valueOf(body.get("toggledBy").toString());
        return ResponseEntity.ok(menuItemService.toggleAvailability(restaurantId, menuItemId, toggledBy));
    }

    // [S2-F9] Get Restaurants with Unavailable Menu Items
    @GetMapping("/menu-items/unavailable")
    public ResponseEntity<List<RestaurantMenuAlertDTO>> getRestaurantsWithUnavailableItems() {
        return ResponseEntity.ok(restaurantService.getRestaurantsWithUnavailableItems());
    }

    // [S2-F10] Full-Text Restaurant Search via Elasticsearch (Milestone 2 §10.2.1)
    @GetMapping("/search/full-text")
    public ResponseEntity<List<RestaurantSearchDocument>> fullTextSearch(
            @RequestParam String query,
            @RequestParam(required = false) String cuisineType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating) {
        return ResponseEntity.ok(
                restaurantService.fullTextSearch(query, cuisineType, status, minRating, maxRating)
        );
    }

    // [S2-F11] Index restaurant for search (Milestone 2 §10.2.2)
    @PostMapping("/{id}/index")
    public ResponseEntity<Void> indexRestaurant(@PathVariable Long id) {
        restaurantService.indexRestaurantForSearch(id);
        return ResponseEntity.ok().build();
    }

    // [S2-F12] Get Restaurant Performance Dashboard
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<RestaurantDashboardDTO> getDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getDashboard(id));
    }
}