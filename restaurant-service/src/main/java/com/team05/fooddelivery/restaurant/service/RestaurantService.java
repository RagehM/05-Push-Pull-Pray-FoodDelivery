package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.restaurant.dto.RestaurantRevenueDTO;
import com.team05.fooddelivery.restaurant.dto.TopRestaurantDTO;
import com.team05.fooddelivery.restaurant.enums.RestaurantStatusEnum;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;
import com.team05.fooddelivery.restaurant.repository.MenuItemRepository;
import com.team05.fooddelivery.restaurant.dto.RestaurantMenuAlertDTO;
import com.team05.fooddelivery.restaurant.util.CacheInvalidationUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team05.fooddelivery.restaurant.model.MenuItem;

import java.util.ArrayList;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final CacheInvalidationUtil cacheInvalidationUtil;

    public RestaurantService(RestaurantRepository restaurantRepository,
                             MenuItemRepository menuItemRepository,
                             CacheInvalidationUtil cacheInvalidationUtil) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.cacheInvalidationUtil = cacheInvalidationUtil;
    }

    public Restaurant create(Restaurant restaurant) {
        if (restaurant.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New restaurant must not have an id");
        }
        Restaurant saved = restaurantRepository.save(restaurant);
        cacheInvalidationUtil.evictByPattern("restaurant-service::S2-F1::*");
        return saved;
    }

    // Cached 15 min — spec Section 4.4.2
    @Cacheable(value = "restaurant-service::restaurant", key = "#id")
    public Restaurant getById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
    }

    // NOT cached — list endpoints never cached, spec Section 4.4.2
    public List<Restaurant> getAll() {
        return restaurantRepository.findAll();
    }

    public Restaurant update(Long id, Restaurant updated) {
        Restaurant existing = getById(id);
        if (updated.getName() != null)
            existing.setName(updated.getName());
        if (updated.getEmail() != null)
            existing.setEmail(updated.getEmail());
        if (updated.getPhone() != null)
            existing.setPhone(updated.getPhone());
        if (updated.getCuisineType() != null)
            existing.setCuisineType(updated.getCuisineType());
        if (updated.getStatus() != null)
            existing.setStatus(updated.getStatus());
        if (updated.getDetails() != null)
            existing.setDetails(updated.getDetails());
        Restaurant saved = restaurantRepository.save(existing);
        invalidateRestaurantCaches(id);
        return saved;
    }

    public void delete(Long id) {
        if (!restaurantRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        restaurantRepository.deleteById(id);
        invalidateRestaurantCaches(id);
    }

    // [S2-F1] Cached 5 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F1", key = "#cuisineType + ':' + #minRating + ':' + #maxRating")
    public List<Restaurant> searchByCuisineAndRating(String cuisineType, Double minRating, Double maxRating) {
        if (minRating > maxRating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating cannot be greater than maxRating");
        }
        return restaurantRepository.searchByCuisineAndRating(cuisineType, minRating, maxRating);
    }

    // [S2-F2] Write — invalidates caches — Section 4.4.4
    public Restaurant updateDetails(Long id, Map<String, Object> newDetails) {
        Restaurant existing = getById(id);
        Map<String, Object> currentDetails = existing.getDetails();
        if (currentDetails == null) {
            existing.setDetails(newDetails);
        } else {
            currentDetails.putAll(newDetails);
            existing.setDetails(currentDetails);
        }
        Restaurant saved = restaurantRepository.save(existing);
        invalidateRestaurantCaches(id);
        return saved;
    }

    // [S2-F3] Cached 10 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F3", key = "#id + ':' + #startDate + ':' + #endDate")
    public RestaurantRevenueDTO getRevenueSummary(Long id, LocalDateTime startDate, LocalDateTime endDate) {
        Restaurant restaurant = getById(id);
        List<Object[]> results = restaurantRepository.getRevenueSummary(id, startDate, endDate);
        Object[] result = results.get(0);
        Long totalOrders = ((Number) result[0]).longValue();
        Double totalRevenue = ((Number) result[1]).doubleValue();
        Double averageOrderAmount = ((Number) result[2]).doubleValue();
        return new RestaurantRevenueDTO(restaurant.getId(), restaurant.getName(), totalOrders, totalRevenue,
                averageOrderAmount);
    }

    // [S2-F4] Write — invalidates caches — Section 4.4.4
    @Transactional
    public void updateRestaurantStatus(Long id, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        if ("SUSPENDED".equals(newStatus)) {
            int activeOrders = restaurantRepository.countActiveOrders(id);
            if (activeOrders > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot suspend restaurant with active orders");
            }
        }
        try {
            restaurant.setStatus(RestaurantStatusEnum.valueOf(newStatus));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + newStatus);
        }
        restaurantRepository.save(restaurant);
        invalidateRestaurantCaches(id);
    }

    // [S2-F5] Cached 5 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F5", key = "#key + ':' + #value + ':' + #status")
    public List<Restaurant> filterByDetail(String key, String value, String status) {
        return restaurantRepository.findByDetailAttribute(key, value, status);
    }

    // [S2-F6] Cached 10 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F6", key = "#limit")
    public List<TopRestaurantDTO> getTopRated(int limit) {
        List<Object[]> results = restaurantRepository.findTopRatedRestaurants(limit);
        List<TopRestaurantDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            String name = (String) row[1];
            Double rating = ((Number) row[2]).doubleValue();
            Long totalOrders = ((Number) row[3]).longValue();
            dtos.add(new TopRestaurantDTO(id, name, rating, totalOrders));
        }
        return dtos;
    }

    // [S2-F7] Write — invalidates caches — Section 4.4.4
    @Transactional
    public void rateRestaurant(Long restaurantId, Long orderId, Integer rating) {
        if (rating == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must not be null");
        }
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }
        Restaurant rest = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        List<Object[]> orderResults = restaurantRepository.findOrderDetailsById(orderId);
        if (orderResults.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        Object[] order = orderResults.get(0);
        Long orderRestaurantId = ((Number) order[0]).longValue();
        String orderStatus = (String) order[1];
        if (!orderRestaurantId.equals(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order does not belong to this restaurant");
        }
        if (!"DELIVERED".equals(orderStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not delivered");
        }
        int newTRating = rest.getTotalRatings() + 1;
        double newRating = ((rest.getRating() * rest.getTotalRatings()) + rating) / newTRating;
        rest.setRating(newRating);
        rest.setTotalRatings(newTRating);
        restaurantRepository.save(rest);
        invalidateRestaurantCaches(restaurantId);
    }

    // [S2-F9] Cached 10 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F9", key = "'all'")
    public List<RestaurantMenuAlertDTO> getRestaurantsWithUnavailableItems() {
        List<Restaurant> restaurants = restaurantRepository.findRestaurantsWithUnavailableItems();
        List<RestaurantMenuAlertDTO> dtos = new ArrayList<>();
        for (Restaurant r : restaurants) {
            List<MenuItem> unavailableItems = menuItemRepository.findByRestaurantIdAndAvailable(r.getId(), false);
            dtos.add(new RestaurantMenuAlertDTO(
                    r.getId(),
                    r.getName(),
                    r.getStatus().toString(),
                    unavailableItems,
                    unavailableItems.size()));
        }
        return dtos;
    }

    // Clears all restaurant-related caches when data changes
    // Section 4.4.4 + 4.4.6
    private void invalidateRestaurantCaches(Long id) {
        cacheInvalidationUtil.evict("restaurant-service::restaurant::" + id);
        cacheInvalidationUtil.evictByPattern("restaurant-service::S2-F1::*");
        cacheInvalidationUtil.evictByPattern("restaurant-service::S2-F3::*");
        cacheInvalidationUtil.evictByPattern("restaurant-service::S2-F5::*");
        cacheInvalidationUtil.evictByPattern("restaurant-service::S2-F6::*");
        cacheInvalidationUtil.evictByPattern("restaurant-service::S2-F9::*");
    }
}