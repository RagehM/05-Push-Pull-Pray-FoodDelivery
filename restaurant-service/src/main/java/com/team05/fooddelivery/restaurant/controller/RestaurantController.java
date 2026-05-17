package com.team05.fooddelivery.restaurant.controller;

import com.team05.fooddelivery.restaurant.dto.RestaurantRevenueDTO;
import com.team05.fooddelivery.restaurant.dto.TopRestaurantDTO;
import com.team05.fooddelivery.contracts.dto.AvgPriceDTO;
import com.team05.fooddelivery.restaurant.dto.RestaurantDashboardDTO;
import com.team05.fooddelivery.restaurant.dto.RestaurantMenuAlertDTO;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.service.RestaurantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.team05.fooddelivery.restaurant.service.MenuItemService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.team05.fooddelivery.restaurant.model.elasticsearch.RestaurantSearchDocument;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private static final Logger log = LoggerFactory.getLogger(RestaurantController.class);

    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;

    public RestaurantController(RestaurantService restaurantService, MenuItemService menuItemService) {
        this.restaurantService = restaurantService;
        this.menuItemService = menuItemService;
    }

    @PostMapping
    public ResponseEntity<Restaurant> create(@RequestBody Restaurant restaurant) {
        log.info("Received POST /api/restaurants");
        Restaurant result = restaurantService.create(restaurant);
        log.info("Returning 200 for POST /api/restaurants");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getById(@PathVariable Long id) {
        log.info("Received GET /api/restaurants/{}", id);
        Restaurant result = restaurantService.getById(id);
        log.info("Returning 200 for GET /api/restaurants/{}", id);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<Restaurant>> getAll() {
        log.info("Received GET /api/restaurants");
        List<Restaurant> result = restaurantService.getAll();
        log.info("Returning 200 for GET /api/restaurants");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Restaurant> update(@PathVariable Long id, @RequestBody Restaurant restaurant) {
        log.info("Received PUT /api/restaurants/{}", id);
        Restaurant result = restaurantService.update(id, restaurant);
        log.info("Returning 200 for PUT /api/restaurants/{}", id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Received DELETE /api/restaurants/{}", id);
        restaurantService.delete(id);
        log.info("Returning 204 for DELETE /api/restaurants/{}", id);
        return ResponseEntity.noContent().build();
    }

    // [S2-F1] Search Restaurants by Cuisine and Rating Range
    @GetMapping("/search")
    public ResponseEntity<List<Restaurant>> search(
            @RequestParam(required = false) String cuisineType,
            @RequestParam Double minRating,
            @RequestParam Double maxRating) {
        log.info("Received GET /api/restaurants/search");
        List<Restaurant> result = restaurantService.searchByCuisineAndRating(cuisineType, minRating, maxRating);
        log.info("Returning 200 for GET /api/restaurants/search");
        return ResponseEntity.ok(result);
    }

    // [S2-F2] Update Restaurant Details (JSONB Partial Update)
    @PutMapping("/{id}/details")
    public ResponseEntity<Restaurant> updateDetails(@PathVariable Long id, @RequestBody Map<String, Object> details) {
        log.info("Received PUT /api/restaurants/{}/details", id);
        Restaurant result = restaurantService.updateDetails(id, details);
        log.info("Returning 200 for PUT /api/restaurants/{}/details", id);
        return ResponseEntity.ok(result);
    }

    // [S2-F3] Get Restaurant Order Revenue Summary (M3: Feign → order-service)
    @GetMapping("/{id}/order-summary")
    public ResponseEntity<RestaurantRevenueDTO> getOrderSummary(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getOrderSummary(id));
    }

    @GetMapping("/{id}/revenue")
    public ResponseEntity<RestaurantRevenueDTO> getRevenueSummary(
            @PathVariable Long id,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("Received GET /api/restaurants/{}/revenue", id);
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
        RestaurantRevenueDTO result = restaurantService.getRevenueSummary(id, start, end);
        log.info("Returning 200 for GET /api/restaurants/{}/revenue", id);
        return ResponseEntity.ok(result);
    }

    // [S2-F4] Update Restaurant Status (Transactional)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("Received PUT /api/restaurants/{}/status", id);
        String status = body.get("status");
        restaurantService.updateRestaurantStatus(id, status);
        log.info("Returning 200 for PUT /api/restaurants/{}/status", id);
        return ResponseEntity.ok().build();
    }

    // [S2-F5] Filter Restaurants by Detail Attribute (JSONB)
    @GetMapping("/details/search")
    public ResponseEntity<List<Restaurant>> filterByDetail(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String status) {
        log.info("Received GET /api/restaurants/details/search");
        List<Restaurant> result = restaurantService.filterByDetail(key, value, status);
        log.info("Returning 200 for GET /api/restaurants/details/search");
        return ResponseEntity.ok(result);
    }

    // [S2-F6] Top Rated Restaurants Report
    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopRestaurantDTO>> getTopRated(@RequestParam int limit) {
        log.info("Received GET /api/restaurants/reports/top-rated");
        List<TopRestaurantDTO> result = restaurantService.getTopRated(limit);
        log.info("Returning 200 for GET /api/restaurants/reports/top-rated");
        return ResponseEntity.ok(result);
    }

    // [S2-F7] Rate a Restaurant After Order (Transactional)
    @PutMapping("/{id}/rate")
    public ResponseEntity<Void> rateRestaurant(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        log.info("Received POST /api/restaurants/{}/rate", id);
        Long orderId = Long.valueOf(body.get("orderId").toString());
        Double rating = Double.valueOf(body.get("rating").toString());
        restaurantService.rateRestaurant(id, orderId, rating);
        log.info("Returning 200 for POST /api/restaurants/{}/rate", id);
        return ResponseEntity.ok().build();
    }

    // [S2-F8] Toggle Menu Item Availability (Transactional)
    @PutMapping("/{restaurantId}/menu-items/{menuItemId}/toggle")
    public ResponseEntity<Restaurant> toggleMenuItemAvailability(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId,
            @RequestBody Map<String, Object> body) {
        log.info("Received PUT /api/restaurants/{}/menu-items/{}/toggle", restaurantId, menuItemId);
        Long toggledBy = Long.valueOf(body.get("toggledBy").toString());
        Restaurant result = menuItemService.toggleAvailability(restaurantId, menuItemId, toggledBy);
        log.info("Returning 200 for PUT /api/restaurants/{}/menu-items/{}/toggle", restaurantId, menuItemId);
        return ResponseEntity.ok(result);
    }

    // [S2-F9] Get Restaurants with Unavailable Menu Items
    @GetMapping("/menu-items/unavailable")
    public ResponseEntity<List<RestaurantMenuAlertDTO>> getRestaurantsWithUnavailableItems() {
        log.info("Received GET /api/restaurants/menu-items/unavailable");
        List<RestaurantMenuAlertDTO> result = restaurantService.getRestaurantsWithUnavailableItems();
        log.info("Returning 200 for GET /api/restaurants/menu-items/unavailable");
        return ResponseEntity.ok(result);
    }

    // [S2-F10] Full-Text Restaurant Search via Elasticsearch
    @GetMapping("/search/full-text")
    public ResponseEntity<List<RestaurantSearchDocument>> fullTextSearch(
            @RequestParam String query,
            @RequestParam(required = false) String cuisineType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating) {
        log.info("Received GET /api/restaurants/search/full-text");
        List<RestaurantSearchDocument> result = restaurantService.fullTextSearch(query, cuisineType, status, minRating, maxRating);
        log.info("Returning 200 for GET /api/restaurants/search/full-text");
        return ResponseEntity.ok(result);
    }

    // [S2-F11] Index restaurant for search
    @PostMapping("/{id}/index")
    public ResponseEntity<Void> indexRestaurant(@PathVariable Long id) {
        log.info("Received POST /api/restaurants/{}/index", id);
        restaurantService.indexRestaurantForSearch(id);
        log.info("Returning 200 for POST /api/restaurants/{}/index", id);
        return ResponseEntity.ok().build();
    }

    // [S2-F12] Get Restaurant Performance Dashboard
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<RestaurantDashboardDTO> getDashboard(@PathVariable Long id) {
        log.info("Received GET /api/restaurants/{}/dashboard", id);
        RestaurantDashboardDTO result = restaurantService.getDashboard(id);
        log.info("Returning 200 for GET /api/restaurants/{}/dashboard", id);
        return ResponseEntity.ok(result);
    }

    // [S2-READ-DB] 
    // Returns AvgPriceDTO {"avgPrice": BigDecimal}
    @GetMapping("/{id}/menu-items/avg-price")
    public ResponseEntity<AvgPriceDTO> getMenuItemsAvgPrice(@PathVariable Long id) {
        log.info("Received GET /api/restaurants/{}/menu-items/avg-price", id);
        AvgPriceDTO result = restaurantService.getMenuItemsAvgPrice(id);
        log.info("Returning 200 for GET /api/restaurants/{}/menu-items/avg-price", id);
        return ResponseEntity.ok(result);
    }
}
