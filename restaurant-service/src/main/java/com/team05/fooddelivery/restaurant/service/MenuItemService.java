package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.restaurant.model.MenuItem;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.repository.MenuItemRepository;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;
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

    // Constructor injection of the MenuItemRepository and RestaurantRepository dependencies
    public MenuItemService(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    // The create method takes a restaurantId and a MenuItem object as input.
    // It first retrieves the Restaurant by its ID. If the restaurant is not found,
    // it throws a ResponseStatusException.
    public MenuItem create(Long restaurantId, MenuItem menuItem) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        menuItem.setRestaurant(restaurant);
        return menuItemRepository.save(menuItem);
    }

    // The getById method retrieves a MenuItem by its ID. If the menu item is not found, it throws a ResponseStatusException.
    public MenuItem getById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MenuItem not found"));
    }

    // The getAll method retrieves all menu items from the database.
    public List<MenuItem> getAll() {
        return menuItemRepository.findAll();
    }

    // The update method updates an existing menu item's details.
    // It first retrieves the existing menu item by its ID. If the menu item is not
    // found, it throws a ResponseStatusException.
    // Then, it updates the existing menu item's fields with the values from the
    // updated menu item object,
    // if they are not null, and saves the updated menu item back to the database.
    // partial update: only non-null fields from the updated object will be applied
    // to the existing menu item
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
        return menuItemRepository.save(existing);
    }

    // The delete method removes a menu item from the database by its ID.
    public void delete(Long id) {
        if (!menuItemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MenuItem not found");
        }
        menuItemRepository.deleteById(id);
    }

    // [S2-F8] Toggle Menu Item Availability (Transactional)
    // Verifies admin permission, restaurant ownership, and no pending order items before toggling availability.
    // Updates the item's metadata with toggledAt and toggledBy, then returns the full updated restaurant.
    @Transactional
    public Restaurant toggleAvailability(Long restaurantId, Long menuItemId, Long toggledBy) {
        // find rest.
        Restaurant rest = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        // find menu item
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MenuItem not found"));
        // check item belongs to this rest.
        if (!menuItem.getRestaurant().getId().equals(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MenuItem does not belong to this restaurant");
        }
        // if its avail.check no pending items
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
        // toggle avail. field
        menuItem.setAvailable(!menuItem.getAvailable());
        // Update metadata
        Map<String, Object> metadata = menuItem.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("toggledAt", LocalDateTime.now().toString());
        metadata.put("toggledBy", toggledBy);
        menuItem.setMetadata(metadata);
        // save item
        menuItemRepository.save(menuItem);
        // return full rest.
        return restaurantRepository.findById(restaurantId).get();
    }
}