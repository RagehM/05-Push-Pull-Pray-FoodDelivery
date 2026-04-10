package com.team05.fooddelivery.restaurant.dto;

import com.team05.fooddelivery.restaurant.model.MenuItem;
import java.util.List;

// [S2-F9] Response DTO for the unavailable menu items alert endpoint.
// Groups a restaurant's basic info with its list of unavailable items and the count of those items.
public class RestaurantMenuAlertDTO {
    private Long restaurantId;
    private String restaurantName;
    private String restaurantStatus;
    private List<MenuItem> unavailableItems;
    private Integer unavailableCount;

    public RestaurantMenuAlertDTO(Long restaurantId, String restaurantName, String restaurantStatus,
            List<MenuItem> unavailableItems, Integer unavailableCount) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.restaurantStatus = restaurantStatus;
        this.unavailableItems = unavailableItems;
        this.unavailableCount = unavailableCount;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public String getRestaurantStatus() {
        return restaurantStatus;
    }

    public List<MenuItem> getUnavailableItems() {
        return unavailableItems;
    }

    public Integer getUnavailableCount() {
        return unavailableCount;
    }
}