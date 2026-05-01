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



@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    // The RestaurantController class is a REST controller that handles HTTP
    // requests related to restaurant operations.
    // It uses the RestaurantService to perform business logic and interact with the
    // database.
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
    // Filters restaurants by optional cuisine type and a rating range, ordered by
    // rating descending.
    // Returns 400 if minRating > maxRating.
    @GetMapping("/search")
    public ResponseEntity<List<Restaurant>> search(
            @RequestParam(required = false) String cuisineType,
            @RequestParam Double minRating,
            @RequestParam Double maxRating) {
        return ResponseEntity.ok(restaurantService.searchByCuisineAndRating(cuisineType, minRating, maxRating));
    }

    // [S2-F2] Update Restaurant Details (JSONB Partial Update)
    // Merges the incoming JSON fields into the restaurant's existing details map
    // Returns 404 if the restaurant is not found.
    @PutMapping("/{id}/details")
    public ResponseEntity<Restaurant> updateDetails(@PathVariable Long id, @RequestBody Map<String, Object> details) {
        return ResponseEntity.ok(restaurantService.updateDetails(id, details));
    }

    // [S2-F3] Get Restaurant Order Revenue Summary
    // Aggregates delivered orders for a restaurant within a date range and returns
    // a revenue summary DTO.
    // Returns 404 if the restaurant is not found.
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
    // Updates the restaurant's status; blocks SUSPENDED if active orders exist
    // Returns 404 if the restaurant is not found.
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        restaurantService.updateRestaurantStatus(id, status);
        return ResponseEntity.ok().build();
    }

    // [S2-F5] Filter Restaurants by Detail Attribute (JSONB)
    // Returns restaurants whose JSONB details map contains the given key-value
    // pair, with an optional status filter.
    @GetMapping("/details/search")
    public ResponseEntity<List<Restaurant>> filterByDetail(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(restaurantService.filterByDetail(key, value, status));
    }

    // [S2-F6] Top Rated Restaurants Report
    // Returns the top N restaurants ordered by rating descending, along with their
    // total order count.
    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopRestaurantDTO>> getTopRated(
            @RequestParam int limit) {
        return ResponseEntity.ok(restaurantService.getTopRated(limit));
    }

    // [S2-F7] Rate a Restaurant After Order (Transactional)
    // Accepts a rating (1–5) for a delivered order and recalculates the
    // restaurant's running average rating.
    // Returns 404 if restaurant or order not found; 400 if order is not delivered,
    // doesn't belong to this restaurant, or rating is out of range.
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
    // Toggles a menu item's availability; blocks if pending orders reference the
    // item (400) or toggler is not an admin (403).
    // Updates the item's metadata with toggledAt timestamp and toggledBy user ID.
    @PutMapping("/{restaurantId}/menu-items/{menuItemId}/toggle")
    public ResponseEntity<Restaurant> toggleMenuItemAvailability(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId,
            @RequestBody Map<String, Object> body) {
        Long toggledBy = Long.valueOf(body.get("toggledBy").toString());
        return ResponseEntity.ok(menuItemService.toggleAvailability(restaurantId, menuItemId, toggledBy));
    }

    // [S2-F9] Get Restaurants with Unavailable Menu Items
    // Returns a list of restaurants that have at least one unavailable menu item,
    // along with those items and their count.
    @GetMapping("/menu-items/unavailable")
    public ResponseEntity<List<RestaurantMenuAlertDTO>> getRestaurantsWithUnavailableItems() {
        return ResponseEntity.ok(restaurantService.getRestaurantsWithUnavailableItems());
    }

    // [S2-F12] Get Restaurant Performance Dashboard
    // Section 10.2.3 — logs DASHBOARD_VIEWED to MongoDB on every call including
    // cache hits
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<RestaurantDashboardDTO> getDashboard(@PathVariable Long id) {
        restaurantService.notifyDashboardViewed(id);

        return ResponseEntity.ok(restaurantService.getDashboard(id));
    }

}