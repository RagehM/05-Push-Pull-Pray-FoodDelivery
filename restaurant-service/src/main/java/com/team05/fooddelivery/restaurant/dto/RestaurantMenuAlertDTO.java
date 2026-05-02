package com.team05.fooddelivery.restaurant.dto;

import com.team05.fooddelivery.restaurant.model.MenuItem;
import java.util.List;

// [S2-F9] Response DTO for the unavailable menu items alert endpoint.
// Builder pattern — Section 3.5
public class RestaurantMenuAlertDTO {

    private Long restaurantId;
    private String restaurantName;
    private String restaurantStatus;
    private List<MenuItem> unavailableItems;
    private Integer unavailableCount;

    private RestaurantMenuAlertDTO(Builder builder) {
        this.restaurantId = builder.restaurantId;
        this.restaurantName = builder.restaurantName;
        this.restaurantStatus = builder.restaurantStatus;
        this.unavailableItems = builder.unavailableItems;
        this.unavailableCount = builder.unavailableCount;
    }

    public Long getRestaurantId() { return restaurantId; }
    public String getRestaurantName() { return restaurantName; }
    public String getRestaurantStatus() { return restaurantStatus; }
    public List<MenuItem> getUnavailableItems() { return unavailableItems; }
    public Integer getUnavailableCount() { return unavailableCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long restaurantId;
        private String restaurantName;
        private String restaurantStatus;
        private List<MenuItem> unavailableItems;
        private Integer unavailableCount;

        public Builder restaurantId(Long restaurantId) { this.restaurantId = restaurantId; return this; }
        public Builder restaurantName(String restaurantName) { this.restaurantName = restaurantName; return this; }
        public Builder restaurantStatus(String restaurantStatus) { this.restaurantStatus = restaurantStatus; return this; }
        public Builder unavailableItems(List<MenuItem> unavailableItems) { this.unavailableItems = unavailableItems; return this; }
        public Builder unavailableCount(Integer unavailableCount) { this.unavailableCount = unavailableCount; return this; }

        public RestaurantMenuAlertDTO build() {
            return new RestaurantMenuAlertDTO(this);
        }
    }
}