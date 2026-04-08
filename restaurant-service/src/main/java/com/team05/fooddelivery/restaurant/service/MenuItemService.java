package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.restaurant.model.MenuItem;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.repository.MenuItemRepository;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    // Constructor injection is used to inject the MenuItemRepository and RestaurantRepository dependencies into the MenuItemService class.
    // This allows the service to interact with the database through the repository layer.

    public MenuItemService(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    // The create method takes a restaurantId and a MenuItem object as input. 
    // It first retrieves the Restaurant associated with the given restaurantId.
    // If the restaurant is not found, it throws a RuntimeException.
    // Then, it sets the restaurant for the MenuItem and saves it to the database.
    public MenuItem create(Long restaurantId, MenuItem menuItem) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        menuItem.setRestaurant(restaurant);
        return menuItemRepository.save(menuItem);
    }

    // The getById method retrieves a MenuItem by its ID. 
    // If the MenuItem is not found, it throws a RuntimeException.
    public MenuItem getById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MenuItem not found"));
    }
    
    // The getAll method retrieves all MenuItems from the database.
    public List<MenuItem> getAll() {
        return menuItemRepository.findAll();
    }

    // The update method updates an existing MenuItem's details.
    public MenuItem update(Long id, MenuItem updated) {
        MenuItem existing = getById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setPrice(updated.getPrice());
        existing.setCategory(updated.getCategory());
        existing.setAvailable(updated.getAvailable());
        existing.setMetadata(updated.getMetadata());
        return menuItemRepository.save(existing);
    }

    // The delete method removes a MenuItem from the database by its ID.
    public void delete(Long id) {
        if (menuItemRepository.existsById(id)) {
            menuItemRepository.deleteById(id);
        }
    }
}