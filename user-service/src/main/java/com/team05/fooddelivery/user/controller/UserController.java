package com.team05.fooddelivery.user.controller;

import com.team05.fooddelivery.user.dto.TopCustomerDTO;
import com.team05.fooddelivery.user.dto.UserOrderSummaryDTO;
import com.team05.fooddelivery.user.dto.UserProfileDTO;
import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.UserRepository;
import com.team05.fooddelivery.user.service.DeliveryAddressService;
import com.team05.fooddelivery.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final DeliveryAddressService deliveryAddressService;

    @Autowired
    public UserController(UserService userService,DeliveryAddressService deliveryAddressService)
    {
        this.userService = userService;
        this.deliveryAddressService=deliveryAddressService;
    }

    @GetMapping
    public List<User> getUsers()
    {
        return userService.findAll();
    }

    @PostMapping
    public User createUser(@RequestBody User user)
    {
        return userService.createUser(user);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable long id)
    {
        return userService.findUserById(id);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable long id, @RequestBody User user)
    {
        return userService.updateUser(user, id);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable long id)
    {
        userService.deleteUser(id);
    }

    @GetMapping("/search")
    public List<User> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role
    ) {
        return userService.searchUsers(name, email, role);
    }
    //feature 1.2
    @PutMapping("/{id}/preferences")
    public ResponseEntity<User> updateUserPreferences(@PathVariable long id, @RequestBody Map<String, Object> preferences)
    {
       return ResponseEntity.ok(userService.updateUserPreferences(preferences, id));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseStatusException deactivateUserAccount(@PathVariable long id){
       return userService.deactivateUserAccount(id);
    }

    @GetMapping("/preferences/search")
    public List<User> getUsersByPreferences(@RequestParam String key, @RequestParam String value)
    {
        return userService.filterUsersByPreferences(key, value);
    }


    @GetMapping("/{id}/order-summary")
    public UserOrderSummaryDTO getUserOrderSummary(@PathVariable long id){
        return userService.getUserOrderSummary(id);
    }


    @GetMapping("/reports/top-customers")
    public List<TopCustomerDTO> topCustomersBySpending(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam Integer limit)
    {
        return userService.topCustomersBySpending(startDate, endDate, limit);
    }
    @GetMapping("/preferences/dietary")
    public List<User> getUsersByPreferenceAndMinimumOrder(@RequestParam String diet, @RequestParam int minOrders)
    {
        return userService.findUsersByPreferencesAndMinimumOrders(diet, minOrders);
    }

    @PostMapping("/{userId}/addresses")
    public DeliveryAddress createUserAddresses(@PathVariable long userId, @RequestBody DeliveryAddress deliveryAddress){
        return deliveryAddressService.createDeliveryAddressForUser(deliveryAddress, userId);
    }
    @GetMapping("/{userId}/addresses")
    public List<DeliveryAddress> getUserAddresses(@PathVariable long userId){
        return userService.getDeliveryAddressesForUser(userId);
    }
    @DeleteMapping("/addresses/{id}")
    public void deleteUserAddresses(@PathVariable long id){
        deliveryAddressService.deleteDeliveryAddress(id);
    }
    @PutMapping("/{userId}/addresses/{addressId}/default")
    public User setDefaultDeliveryAddress(@PathVariable long userId, @PathVariable long addressId)
    {
        return userService.setDefaultDeliveryAddress(userId, addressId);
    }

    @GetMapping("/{id}/profile")
    public UserProfileDTO getUserProfile(@PathVariable long id) {
        return userService.getUserProfile(id);
    }
}