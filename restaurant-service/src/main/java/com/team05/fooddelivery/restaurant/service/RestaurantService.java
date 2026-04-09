package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.restaurant.dto.RestaurantRevenueDTO;
import com.team05.fooddelivery.restaurant.dto.TopRestaurantDTO;
import com.team05.fooddelivery.restaurant.enums.RestaurantStatusEnum;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RestaurantService {

    // The RestaurantService class is annotated with @Service, indicating that it's
    // a service component in the Spring framework.
    // It contains business logic related to restaurant operations and interacts
    // with the RestaurantRepository to perform database operations.
    private final RestaurantRepository restaurantRepository;

    // Constructor injection of the RestaurantRepository dependency
    // allows the service to interact with the database through the repository
    // layer.
    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    // The create method takes a Restaurant object as input and saves it to the
    // database using the restaurantRepository's save method.
    public Restaurant create(Restaurant restaurant) {
        if (restaurant.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New restaurant must not have an id");
        }
        return restaurantRepository.save(restaurant);
    }

    // The getById method retrieves a Restaurant by its ID. If the restaurant is not
    // found, it throws a ResponseStatusException.
    public Restaurant getById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
    }

    // The getAll method retrieves all restaurants from the database.
    public List<Restaurant> getAll() {
        return restaurantRepository.findAll();
    }

    // The update method updates an existing restaurant's details.
    // It first retrieves the existing restaurant by its ID. If the restaurant is
    // not found, it throws a ResponseStatusException.
    // Then, it updates the existing restaurant's fields with the values from the
    // updated restaurant object,
    // if they are not null, and saves the updated restaurant back to the database.
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
        return restaurantRepository.save(existing);
    }

    // The delete method removes a restaurant from the database by its ID.
    public void delete(Long id) {
        if (!restaurantRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        restaurantRepository.deleteById(id);
    }

    // S2-F1: Implement search functionality in RestaurantService to allow filtering
    // by cuisine type and rating range.
    public List<Restaurant> searchByCuisineAndRating(String cuisineType, Double minRating, Double maxRating) {
        if (minRating > maxRating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating cannot be greater than maxRating");
        }
        return restaurantRepository.searchByCuisineAndRating(cuisineType, minRating, maxRating);
    }

    // S2-F2: Implement a method in RestaurantService to update only specific
    // details of a restaurant,
    // such as its contact information or operating hours, without affecting other
    // fields.
    public Restaurant updateDetails(Long id, Map<String, Object> newDetails) {
        Restaurant existing = getById(id);
        Map<String, Object> currentDetails = existing.getDetails();
        if (currentDetails == null) {
            existing.setDetails(newDetails);
        } else {
            currentDetails.putAll(newDetails);
            existing.setDetails(currentDetails);
        }
        return restaurantRepository.save(existing);
    }

    // S2-F3: Implement a method in RestaurantService to calculate the total revenue
    // generated by a restaurant within a specified date range,
    // using the custom query defined in RestaurantRepository.
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
    }

    //S2-F5
    public List<Restaurant> filterByDetail(String key, String value, String status) {
        return restaurantRepository.findByDetailAttribute(key, value, status);
    }
    //s2-f6
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

}