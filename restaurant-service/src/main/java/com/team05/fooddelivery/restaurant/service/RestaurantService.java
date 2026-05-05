package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.restaurant.adapter.RestaurantRevenueAdapter;
import com.team05.fooddelivery.restaurant.adapter.TopRestaurantAdapter;
import com.team05.fooddelivery.restaurant.dto.RestaurantDashboardDTO;
import com.team05.fooddelivery.restaurant.dto.RestaurantMenuAlertDTO;
import com.team05.fooddelivery.restaurant.dto.RestaurantRevenueDTO;
import com.team05.fooddelivery.restaurant.dto.TopRestaurantDTO;
import com.team05.fooddelivery.restaurant.enums.RestaurantStatusEnum;
import com.team05.fooddelivery.restaurant.model.MenuItem;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.repository.MenuItemRepository;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;
import com.team05.fooddelivery.restaurant.repository.mongo.MongoRestaurantEventRepository;
import com.team05.shared.model.mongo.MongoEvent.EventType;
import com.team05.shared.model.mongo.RestaurantEvent.RestaurantEventActions;
import com.team05.shared.observer.EntityObserver;
import com.team05.shared.observer.MongoEventLogger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
//    private final EventFactory eventFactory = new EventFactory();
    private final CacheManager cacheManager;
    private final RestaurantElasticsearchIndexService restaurantElasticsearchIndexService;

    public RestaurantService(RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            MongoRestaurantEventRepository mongoRestaurantEventRepository,
            RestaurantElasticsearchIndexService restaurantElasticsearchIndexService, {
            MongoRestaurantEventRepository mongoRestaurantEventRepository,
            CacheManager cacheManager) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.cacheManager = cacheManager;

        this.restaurantElasticsearchIndexService = restaurantElasticsearchIndexService;
        // Register the MongoEventLogger observer — bound to RESTAURANT event type
        // Section 3.3 + 4.5
        this.observers.add(
                new MongoEventLogger<>(mongoRestaurantEventRepository, EventType.RESTAURANT));
    }

    // CRUD create — no cache eviction (spec Section 4.4.4)
    public Restaurant create(Restaurant restaurant) {
        if (restaurant.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New restaurant must not have an id");
        }

        // Section 7.2 — ensure details.description key exists with default empty string
        Map<String, Object> details = restaurant.getDetails();
        if (details == null) {
            details = new HashMap<>();
        }
        details.putIfAbsent("description", "");
        restaurant.setDetails(details);

        Restaurant saved = restaurantRepository.save(restaurant);

        // Notify observers — Section 4.5
        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", saved.getId());
        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("name", saved.getName());
        eventDetails.put("cuisineType", saved.getCuisineType());
        params.put("details", eventDetails);
        notifyObservers(RestaurantEventActions.RESTAURANT_CREATED, params);
        //s2-f11
        restaurantElasticsearchIndexService.upsertFromRestaurant(saved);

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

    // CRUD update — evict caches + notify observers
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::restaurant", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F1", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F3", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F5", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F6", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#id")
    })

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

        Map<String, Object> params = new HashMap<>();
        params.put("action", RestaurantEventActions.UPDATED);
        params.put("restaurantId", saved.getId());
        Map<String, Object> details = new HashMap<>();
        details.put("name", saved.getName());
        details.put("status", saved.getStatus());
        params.put("details", details);
        notifyObservers(RestaurantEventActions.UPDATED, params);
        //s2-f11
        restaurantElasticsearchIndexService.upsertFromRestaurant(saved);
        return saved;
    }

    // CRUD delete — evict caches + notify observers
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::restaurant", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F1", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F3", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F5", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F6", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#id")
    })
    public void delete(Long id) {
        if (!restaurantRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        restaurantRepository.deleteById(id);
        //s2-f11
        restaurantElasticsearchIndexService.deleteByRestaurantId(id);

        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", id);
        params.put("details", new HashMap<>());
        notifyObservers(RestaurantEventActions.RESTAURANT_DELETED, params);
    }

    // [S2-F1] Cached 5 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F1", key = "#cuisineType + ':' + #minRating + ':' + #maxRating")
    public List<Restaurant> searchByCuisineAndRating(String cuisineType, Double minRating, Double maxRating) {
        if (minRating > maxRating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating cannot be greater than maxRating");
        }
        return restaurantRepository.searchByCuisineAndRating(cuisineType, minRating, maxRating);
    }

    // [S2-F2] Write — invalidates caches + notify observers — Section 4.4.4
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::restaurant", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F1", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F3", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F5", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F6", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#id")
    })
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
        //s2-f11
        restaurantElasticsearchIndexService.upsertFromRestaurant(saved);

        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", saved.getId());
        params.put("details", newDetails);
        notifyObservers(RestaurantEventActions.DETAILS_UPDATED, params);

        return saved;
    }

    // [S2-F3] Cached 10 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F3", key = "#id + ':' + #startDate + ':' + #endDate")
    public RestaurantRevenueDTO getRevenueSummary(Long id, LocalDateTime startDate, LocalDateTime endDate) {
        Restaurant restaurant = getById(id);
        List<Object[]> results = restaurantRepository.getRevenueSummary(id, startDate, endDate);
        return new RestaurantRevenueAdapter(results.get(0), restaurant).adapt(results.get(0));
    }

    // [S2-F4] Write — invalidates caches + notify observers — Section 4.4.4
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::restaurant", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F1", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F3", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F5", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F6", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#id")
    })
    public void updateRestaurantStatus(Long id, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
                // CLOSE added for TC 246
        if ("SUSPENDED".equals(newStatus) || "CLOSED".equals(newStatus) ) {
            int activeOrders = restaurantRepository.countActiveOrders(id);
            if (activeOrders > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot suspend or close restaurant with active orders");
            }
        }
        try {
            restaurant.setStatus(RestaurantStatusEnum.valueOf(newStatus));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + newStatus);
        }
        restaurantRepository.save(restaurant);
        //s2-f11
        restaurantElasticsearchIndexService.upsertFromRestaurant(restaurant);

        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", id);
        Map<String, Object> details = new HashMap<>();
        details.put("newStatus", newStatus);
        params.put("details", details);
        notifyObservers(RestaurantEventActions.STATUS_CHANGED, params);
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
            dtos.add(new TopRestaurantAdapter(row).adapt(row));
        }
        return dtos;
    }

    // [S2-F7] Write — invalidates caches + notify observers — Section 4.4.4
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::restaurant", key = "#restaurantId"),
            @CacheEvict(value = "restaurant-service::S2-F1", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F3", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F5", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F6", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#restaurantId")
    })
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
        //s2-f11
        restaurantElasticsearchIndexService.upsertFromRestaurant(rest);

        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", restaurantId);
        Map<String, Object> details = new HashMap<>();
        details.put("rating", rating);
        details.put("orderId", orderId);
        params.put("details", details);
        notifyObservers(RestaurantEventActions.REVIEW_ADDED, params);
    }

    // [S2-F9] Cached 10 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F9", key = "'all'")
    public List<RestaurantMenuAlertDTO> getRestaurantsWithUnavailableItems() {
        List<Restaurant> restaurants = restaurantRepository.findRestaurantsWithUnavailableItems();
        List<RestaurantMenuAlertDTO> dtos = new ArrayList<>();
        for (Restaurant r : restaurants) {
            List<MenuItem> unavailableItems = menuItemRepository.findByRestaurantIdAndAvailable(r.getId(), false);
            dtos.add(RestaurantMenuAlertDTO.builder()
                    .restaurantId(r.getId())
                    .restaurantName(r.getName())
                    .restaurantStatus(r.getStatus().toString())
                    .unavailableItems(unavailableItems)
                    .unavailableCount(unavailableItems.size())
                    .build());
        }
        return dtos;
    }
    //s2-f11
    public void indexRestaurantForSearch(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        restaurantElasticsearchIndexService.upsertFromRestaurant(restaurant);
    }

        // [S2-F12] Get Restaurant Performance Dashboard
// Uses CacheManager
// Section 10.2.3 — cached 10 min
        public RestaurantDashboardDTO getDashboard(Long id) {
            // Step 1 — logging ALWAYS runs (even on cache hits)
            notifyDashboardViewed(id);

            // Step 2 — check cache manually
            Cache cache = cacheManager.getCache("restaurant-service::S2-F12");
            if (cache != null) {
                RestaurantDashboardDTO cached = cache.get(id, RestaurantDashboardDTO.class);
                if (cached != null) {
                    return cached; // cache hit
                }
            }

            // Step 3 — cache miss — fetch from DB
            Restaurant restaurant = getById(id);
            List<Object[]> stats = restaurantRepository.getDashboardOrderStats(id);
            Object[] row = stats.get(0);
            Long totalOrders = ((Number) row[0]).longValue();
            Double totalRevenue = ((Number) row[1]).doubleValue();
            Double averageOrderValue = ((Number) row[2]).doubleValue();
            Long activeMenuItems = restaurantRepository.countActiveMenuItems(id);

            RestaurantDashboardDTO dto = RestaurantDashboardDTO.builder()
                    .restaurantId(restaurant.getId())
                    .name(restaurant.getName())
                    .totalOrders(totalOrders)
                    .totalRevenue(totalRevenue)
                    .averageOrderValue(averageOrderValue)
                    .activeMenuItems(activeMenuItems)
                    .build();

            // Step 4 — store in cache
            if (cache != null) {
                cache.put(id, dto);
            }

            return dto;
        }

        // [S2-F12] Logs DASHBOARD_VIEWED event to MongoDB — called on every request
        // including cache hits
        // Section 10.2.3 — pure observability, does NOT invalidate cache
        public void notifyDashboardViewed(Long restaurantId) {
            Map<String, Object> params = new HashMap<>();
            params.put("action", RestaurantEventActions.DASHBOARD_VIEWED);
            params.put("restaurantId", restaurantId);
            params.put("details", new HashMap<>());
            notifyObservers(RestaurantEventActions.DASHBOARD_VIEWED, params);
        }

    public void registerObserver(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }


}