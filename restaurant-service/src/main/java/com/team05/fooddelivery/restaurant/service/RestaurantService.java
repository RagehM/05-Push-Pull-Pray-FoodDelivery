package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        if (restaurant.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New restaurant must not have an id");
        }
        return restaurantRepository.save(restaurant);
    }

    // The getById method retrieves a Restaurant by its ID. If the restaurant is not found, it throws a ResponseStatusException.
    public Restaurant getById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
    }

    // The getAll method retrieves all restaurants from the database.
    public List<Restaurant> getAll() {
        return restaurantRepository.findAll();
    }

    // The update method updates an existing restaurant's details.
    // It first retrieves the existing restaurant by its ID. If the restaurant is not found, it throws a ResponseStatusException.
    // Then, it updates the existing restaurant's fields with the values from the updated restaurant object,
    // if they are not null, and saves the updated restaurant back to the database.
    public Restaurant update(Long id, Restaurant updated) {
        Restaurant existing = getById(id);
        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail());
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
        if (updated.getCuisineType() != null) existing.setCuisineType(updated.getCuisineType());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        if (updated.getDetails() != null) existing.setDetails(updated.getDetails());
        return restaurantRepository.save(existing);
    }

    // The delete method removes a restaurant from the database by its ID.
    public void delete(Long id) {
        if (!restaurantRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        restaurantRepository.deleteById(id);
    }

    //S2-F1
    public List<Restaurant> searchByCuisineAndRating(String cuisineType, Double minRating, Double maxRating) {
    if (minRating > maxRating) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating cannot be greater than maxRating");
    }
    return restaurantRepository.searchByCuisineAndRating(cuisineType, minRating, maxRating);
}
}