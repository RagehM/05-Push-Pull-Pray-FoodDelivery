package com.team05.fooddelivery.user.controller;

import com.team05.fooddelivery.user.dto.*;
import com.team05.fooddelivery.user.enums.UserRole;
import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.UserRepository;
import com.team05.fooddelivery.user.service.DeliveryAddressService;
import com.team05.fooddelivery.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    public UserController(UserService userService,DeliveryAddressService deliveryAddressService)
    {
        this.userService = userService;
        this.deliveryAddressService=deliveryAddressService;
    }

    @GetMapping
    public List<User> getUsers() {
        log.info("Received {} {}", "GET", "/api/users");
        List<User> result = userService.findAll();
        log.info("Returning {} for {} {}", 200, "GET", "/api/users");
        return result;
    }

    @PostMapping
    public User createUser(@RequestBody User user)
    {
        log.info("Received {} {}", "POST", "/api/users");
        User result = userService.createUser(user);
        log.info("Returning {} for {} {}", 200, "POST", "/api/users");
        return result;
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable long id,
                            @RequestHeader(value = "X-User-Id", required = false) Long callerUserId,
                            @RequestHeader(value = "X-User-Role", required = false) String callerRole)
    {
        log.info("Received {} {}", "GET", "/api/users/" + id);
        User result = userService.findUserById(id, callerUserId, callerRole);
        log.info("Returning {} for {} {}", 200, "GET", "/api/users/" + id);
        return result;
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable long id, @RequestBody User user,
                           @RequestHeader(value = "X-User-Id", required = false) Long callerUserId,
                           @RequestHeader(value = "X-User-Role", required = false) String callerRole)
    {
        log.info("Received {} {}", "PUT", "/api/users/" + id);
        User result = userService.updateUser(user, id, callerUserId, callerRole);
        log.info("Returning {} for {} {}", 200, "PUT", "/api/users/" + id);
        return result;
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable long id,
                           @RequestHeader(value = "X-User-Id", required = false) Long callerUserId,
                           @RequestHeader(value = "X-User-Role", required = false) String callerRole)
    {
        log.info("Received {} {}", "DELETE", "/api/users/" + id);
        userService.deleteUser(id, callerUserId, callerRole);
        log.info("Returning {} for {} {}", 204, "DELETE", "/api/users/" + id);
    }

    @GetMapping("/search")
    public List<User> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role)
    {
        log.info("Received {} {}", "GET", "/api/users/search");
        List<User> result = userService.searchUsers(name, email, role);
        log.info("Returning {} for {} {}", 200, "GET", "/api/users/search");
        return result;
    }

    @PutMapping("/{id}/preferences")
    public ResponseEntity<User> updateUserPreferences(@PathVariable long id, @RequestBody Map<String, Object> preferences)
    {
        log.info("Received {} {}", "PUT", "/api/users/" + id + "/preferences");
        ResponseEntity<User> result = ResponseEntity.ok(userService.updateUserPreferences(preferences, id));
        log.info("Returning {} for {} {}", 200, "PUT", "/api/users/" + id + "/preferences");
        return result;
    }

    @PutMapping("/{id}/deactivate")
    public ResponseStatusException deactivateUserAccount(@PathVariable long id)
    {
        log.info("Received {} {}", "PUT", "/api/users/" + id + "/deactivate");
        ResponseStatusException result = userService.deactivateUserAccount(id);
        log.info("Returning {} for {} {}", 200, "PUT", "/api/users/" + id + "/deactivate");
        return result;
    }

    @GetMapping("/preferences/search")
    public List<User> getUsersByPreferences(@RequestParam String key, @RequestParam String value)
    {
        log.info("Received {} {}", "GET", "/api/users/preferences/search");
        List<User> result = userService.filterUsersByPreferences(key, value);
        log.info("Returning {} for {} {}", 200, "GET", "/api/users/preferences/search");
        return result;
    }

    @GetMapping("/{id}/order-summary")
    public UserOrderSummaryDTO getUserOrderSummary(@PathVariable long id){
        log.info("Received {} {}", "GET", "/api/users/" + id + "/order-summary");
        UserOrderSummaryDTO result = userService.getUserOrderSummary(id);
        log.info("Returning {} for {} {}", 200, "GET", "/api/users/" + id + "/order-summary");
        return result;
    }

    @GetMapping("/reports/top-customers")
    public List<TopCustomerDTO> topCustomersBySpending(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam Integer limit)
    {
        log.info("Received {} {}", "GET", "/api/users/reports/top-customers");
        List<TopCustomerDTO> result = userService.topCustomersBySpending(startDate, endDate, limit);
        log.info("Returning {} for {} {}", 200, "GET", "/api/users/reports/top-customers");
        return result;
    }

    @GetMapping("/preferences/dietary")
    public List<User> getUsersByPreferenceAndMinimumOrder(@RequestParam String diet, @RequestParam int minOrders)
    {
        log.info("Received {} {}", "GET", "/api/users/preferences/dietary");
        List<User> result = userService.findUsersByPreferencesAndMinimumOrders(diet, minOrders);
        log.info("Returning {} for {} {}", 200, "GET", "/api/users/preferences/dietary");
        return result;
    }

    @PostMapping("/{userId}/addresses")
    public DeliveryAddress createUserAddresses(@PathVariable long userId, @RequestBody DeliveryAddress deliveryAddress){
        log.info("Received {} {}", "POST", "/api/users/" + userId + "/addresses");
        DeliveryAddress result = deliveryAddressService.createDeliveryAddressForUser(deliveryAddress, userId);
        log.info("Returning {} for {} {}", 200, "POST", "/api/users/" + userId + "/addresses");
        return result;
    }

    @GetMapping("/{userId}/addresses")
    public List<DeliveryAddress> getUserAddresses(@PathVariable long userId){
        log.info("Received {} {}", "GET", "/api/users/" + userId + "/addresses");
        List<DeliveryAddress> result = userService.getDeliveryAddressesForUser(userId);
        log.info("Returning {} for {} {}", 200, "GET", "/api/users/" + userId + "/addresses");
        return result;
    }

    @DeleteMapping("/addresses/{id}")
    public void deleteUserAddresses(@PathVariable long id){
        log.info("Received {} {}", "DELETE", "/api/users/addresses/" + id);
        deliveryAddressService.deleteDeliveryAddress(id);
        log.info("Returning {} for {} {}", 204, "DELETE", "/api/users/addresses/" + id);
    }

    @PutMapping("/{userId}/addresses/{addressId}/default")
    public User setDefaultDeliveryAddress(@PathVariable long userId, @PathVariable long addressId)
    {
        log.info("Received {} {}", "PUT", "/api/users/" + userId + "/addresses/" + addressId + "/default");
        User result = userService.setDefaultDeliveryAddress(userId, addressId);
        log.info("Returning {} for {} {}", 200, "PUT", "/api/users/" + userId + "/addresses/" + addressId + "/default");
        return result;
    }

    @GetMapping("/{id}/profile")
    public UserProfileDTO getUserProfile(@PathVariable long id) {
        log.info("Received {} {}", "GET", "/api/users/" + id + "/profile");
        UserProfileDTO result = userService.getUserProfile(id);
        log.info("Returning {} for {} {}", 200, "GET", "/api/users/" + id + "/profile");
        return result;
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUserRole(@PathVariable long id, @RequestBody RoleUpdateRequest request) {
        log.info("Received {} {}", "PUT", "/api/users/" + id + "/role");
        User result = userService.updateUserRole(id, request.role);
        log.info("Returning {} for {} {}", 200, "PUT", "/api/users/" + id + "/role");
        return result;
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<ActivityFeedDTO> getUserActivityFeed(@PathVariable long id,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestHeader(value = "X-User-Id", required = false) Long callerUserId,
                                                               @RequestHeader(value = "X-User-Role", required = false) String callerRole)
    {
        log.info("Received {} {}", "GET", "/api/users/" + id + "/activity");
        ResponseEntity<ActivityFeedDTO> result = ResponseEntity.ok(userService.getUserActivityFeed(id, page, size, callerUserId, callerRole));
        log.info("Returning {} for {} {}", 200, "GET", "/api/users/" + id + "/activity");
        return result;
    }
}