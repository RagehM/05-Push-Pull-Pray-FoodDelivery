package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.restaurant.model.MenuItem;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.repository.MenuItemRepository;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;
import com.team05.fooddelivery.restaurant.util.CacheInvalidationUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final CacheInvalidationUtil cacheInvalidationUtil;

    public MenuItemService(MenuItemRepository menuItemRepository,
                           RestaurantRepository restaurantRepository,
                           CacheInvalidationUtil cacheInvalidationUtil) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.cacheInvalidationUtil = cacheInvalidationUtil;
    }

    // No cache to evict on create — new item has no cached detail yet
    public MenuItem create(Long restaurantId, MenuItem menuItem) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        menuItem.setRestaurant(restaurant);
        MenuItem saved = menuItemRepository.save(menuItem);
        // Invalidate S2-F9 since it lists restaurants with unavailable items
        cacheInvalidationUtil.evictByPattern("restaurant-service::S2-F9::*");
        return saved;
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

    public MenuItem update(Long id, MenuItem updated) {
        MenuItem existing = getById(id);
        if (updated.getName() != null)
            existing.setName(updated.getName());
        if (updated.getDescription() != null)
            existing.setDescription(updated.getDescription());
        if (updated.getPrice() != null)
            existing.setPrice(updated.getPrice());
        if (updated.getCategory() != null)
            existing.setCategory(updated.getCategory());
        if (updated.getAvailable() != null)
            existing.setAvailable(updated.getAvailable());
        if (updated.getMetadata() != null)
            existing.setMetadata(updated.getMetadata());
        MenuItem saved = menuItemRepository.save(existing);
        invalidateMenuItemCaches(id);
        return saved;
    }

    public void delete(Long id) {
        if (!menuItemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MenuItem not found");
        }
        menuItemRepository.deleteById(id);
        invalidateMenuItemCaches(id);
    }

    // [S2-F8] Write — invalidates caches 
    @Transactional
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
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("toggledAt", LocalDateTime.now().toString());
        metadata.put("toggledBy", toggledBy);
        menuItem.setMetadata(metadata);
        menuItemRepository.save(menuItem);
        invalidateMenuItemCaches(menuItemId);
        return restaurantRepository.findById(restaurantId).get();
    }

    // Clears all menu-item-related caches when data changes
    // Spec ref: Section 4.4.4 + 4.4.6
    private void invalidateMenuItemCaches(Long id) {
        cacheInvalidationUtil.evict("restaurant-service::menu-item::" + id);
        cacheInvalidationUtil.evictByPattern("restaurant-service::S2-F9::*");
    }
}