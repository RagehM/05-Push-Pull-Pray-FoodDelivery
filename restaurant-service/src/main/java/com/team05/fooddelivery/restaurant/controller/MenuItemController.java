package com.team05.fooddelivery.restaurant.controller;

import com.team05.fooddelivery.restaurant.model.MenuItem;
import com.team05.fooddelivery.restaurant.service.MenuItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
public class MenuItemController {

    private final MenuItemService menuItemService;

    // The MenuItemController class is a REST controller that handles HTTP requests
    // related to menu item operations.
    // It uses the MenuItemService to perform business logic and interact with the
    // database.
    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    // The create method handles POST requests to create a new menu item for a
    // specific restaurant.
    @PostMapping("/restaurant/{restaurantId}")
    public ResponseEntity<MenuItem> create(@PathVariable Long restaurantId, @RequestBody MenuItem menuItem) {
        return ResponseEntity.ok(menuItemService.create(restaurantId, menuItem));
    }

    // The getById method handles GET requests to retrieve a menu item by its ID.
    @GetMapping("/{id}")
    public ResponseEntity<MenuItem> getById(@PathVariable Long id) {
        return ResponseEntity.ok(menuItemService.getById(id));
    }

    // The getAll method handles GET requests to retrieve all menu items.
    @GetMapping
    public ResponseEntity<List<MenuItem>> getAll() {
        return ResponseEntity.ok(menuItemService.getAll());
    }

    // The update method handles PUT requests to update an existing menu item.
    @PutMapping("/{id}")
    public ResponseEntity<MenuItem> update(@PathVariable Long id, @RequestBody MenuItem menuItem) {
        return ResponseEntity.ok(menuItemService.update(id, menuItem));
    }

    // The delete method handles DELETE requests to remove a menu item by its ID.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        menuItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}