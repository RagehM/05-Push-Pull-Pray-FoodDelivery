package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.restaurant.factory.EventFactory;
import com.team05.fooddelivery.restaurant.model.MenuItem;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.model.mongo.RestaurantEvent.RestaurantEventActions;
import com.team05.fooddelivery.restaurant.repository.MenuItemRepository;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;
import com.team05.fooddelivery.restaurant.repository.mongo.MongoRestaurantEventRepository;
import com.team05.shared.model.mongo.MongoEvent.EventType;
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

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final EventFactory eventFactory = new EventFactory();

    public MenuItemService(MenuItemRepository menuItemRepository,
                           RestaurantRepository restaurantRepository,
                           MongoRestaurantEventRepository mongoRestaurantEventRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        // Register the MongoEventLogger observer — bound to RESTAURANT event type
        // Section 3.3 + 4.5
        this.observers.add(
            new MongoEventLogger<>(mongoRestaurantEventRepository, EventType.RESTAURANT, eventFactory)
        );
    }

    // Create — no cache eviction (spec Section 4.4.4)
    public MenuItem create(Long restaurantId, MenuItem menuItem) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        menuItem.setRestaurant(restaurant);
        return menuItemRepository.save(menuItem);
    }

    // Cached 15 min — Section 4.4.2
    @Cacheable(value = "restaurant-service::menu-item", key = "#id")
    public MenuItem getById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MenuItem not found"));
    }

    // NOT cached — list endpoints never cached, Section 4.4.2
    public List<MenuItem> getAll() {
        return menuItemRepository.findAll();
    }

    // Update — evict caches
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::menu-item", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true)
    })
    public MenuItem update(Long id, MenuItem updated) {
        MenuItem existing = getById(id);
        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getPrice() != null) existing.setPrice(updated.getPrice());
        if (updated.getCategory() != null) existing.setCategory(updated.getCategory());
        if (updated.getAvailable() != null) existing.setAvailable(updated.getAvailable());
        if (updated.getMetadata() != null) existing.setMetadata(updated.getMetadata());
        return menuItemRepository.save(existing);
    }

    // Delete — evict caches
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::menu-item", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true)
    })
    public void delete(Long id) {
        if (!menuItemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MenuItem not found");
        }
        menuItemRepository.deleteById(id);
    }

    // [S2-F8] Write — invalidates caches + notify observers — Section 4.5
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::menu-item", key = "#menuItemId"),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true)
    })
    public Restaurant toggleAvailability(Long restaurantId, Long menuItemId, Long toggledBy) {
        Restaurant rest = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MenuItem not found"));
        if (menuItem.getRestaurant() == null || !menuItem.getRestaurant().getId().equals(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MenuItem does not belong to this restaurant");
        }
        if (menuItem.getAvailable()) {
            int pendingCount = menuItemRepository.countPendingOrderItems(menuItemId);
            if (pendingCount > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot disable menu item with pending orders");
            }
        }
        int adminCount = restaurantRepository.countAdminById(toggledBy);
        if (adminCount == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can toggle menu item availability");
        }
        menuItem.setAvailable(!menuItem.getAvailable());
        Map<String, Object> metadata = menuItem.getMetadata();
        if (metadata == null) metadata = new HashMap<>();
        metadata.put("toggledAt", LocalDateTime.now().toString());
        metadata.put("toggledBy", toggledBy);
        menuItem.setMetadata(metadata);
        menuItemRepository.save(menuItem);

        // Notify observers — Section 4.5
        Map<String, Object> params = new HashMap<>();
        params.put("action", RestaurantEventActions.MENU_ITEM_TOGGLED);
        params.put("restaurantId", restaurantId);
        Map<String, Object> details = new HashMap<>();
        details.put("menuItemId", menuItemId);
        details.put("available", menuItem.getAvailable());
        details.put("toggledBy", toggledBy);
        params.put("details", details);
        notifyObservers(RestaurantEventActions.MENU_ITEM_TOGGLED, params);

        return restaurantRepository.findById(restaurantId).get();
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