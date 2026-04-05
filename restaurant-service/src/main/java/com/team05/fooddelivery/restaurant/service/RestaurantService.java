package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.Repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RestaurantService {

    // The RestaurantService class is annotated with @Service, indicating that it's a service component in the Spring framework.
    // It contains business logic related to restaurant operations and interacts with the RestaurantRepository to perform database operations.
    private final RestaurantRepository restaurantRepository;

    // Constructor injection of the RestaurantRepository dependency 
    // allows the service to interact with the database through the repository layer.
    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    // The create method takes a Restaurant object as input and saves it to the database using the restaurantRepository's save method.
    public Restaurant create(Restaurant restaurant) {
        return restaurantRepository.save(restaurant);
    }

    // The getById method retrieves a Restaurant by its ID. If the restaurant is not found, it throws a RuntimeException.
    public Restaurant getById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
    }

    // The getAll method retrieves all restaurants from the database.
    public List<Restaurant> getAll() {
        return restaurantRepository.findAll();
    }

    // The update method updates an existing restaurant's details.
    public Restaurant update(Long id, Restaurant updated) {
        Restaurant existing = getById(id);
        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setCuisineType(updated.getCuisineType());
        existing.setStatus(updated.getStatus());
        existing.setDetails(updated.getDetails());
        return restaurantRepository.save(existing);
    }

    // The delete method removes a restaurant from the database by its ID.
    public void delete(Long id) {
        restaurantRepository.deleteById(id);
    }
}