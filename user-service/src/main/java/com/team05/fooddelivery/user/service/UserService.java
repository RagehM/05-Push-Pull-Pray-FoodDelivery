package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.contracts.dto.OrderSummaryDTO;
import com.team05.fooddelivery.contracts.feign.CheckoutServiceClient;
import com.team05.fooddelivery.contracts.feign.OrderServiceClient;
import com.team05.fooddelivery.user.dto.*;
import com.team05.fooddelivery.user.dto.TopCustomerDTO;
import com.team05.fooddelivery.user.enums.UserRole;
import com.team05.fooddelivery.user.enums.UserStatus;
import com.team05.fooddelivery.user.messaging.publishers.UserEventPublisher;
import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.DeliveryAddressRepository;
import com.team05.fooddelivery.user.repository.UserRepository;
import com.team05.fooddelivery.user.repository.mongo.AuthEventRepository;
import com.team05.shared.model.mongo.AuthEvent;
import com.team05.shared.model.mongo.MongoEvent;
import com.team05.shared.observer.EntityObserver;
import com.team05.shared.observer.MongoEventLogger;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final AuthEventRepository authEventRepository;
    private final MongoTemplate mongoTemplate;
    private final UserEventPublisher publisher;
    private final OrderServiceClient orderServiceClient;
    private final CheckoutServiceClient checkoutServiceClient;

    @Autowired
    public UserService(UserRepository userRepository, DeliveryAddressRepository deliveryAddressRepository, AuthEventRepository authEventRepository, MongoTemplate mongoTemplate, UserEventPublisher publisher, OrderServiceClient orderServiceClient, CheckoutServiceClient checkoutServiceClient) {
        this.userRepository = userRepository;
        this.deliveryAddressRepository=deliveryAddressRepository;
        this.authEventRepository = authEventRepository;
        this.publisher = publisher;
        this.checkoutServiceClient = checkoutServiceClient;
        this.observers.add(
                new MongoEventLogger<>(this.authEventRepository, MongoEvent.EventType.AUTH)
        );
        this.mongoTemplate = mongoTemplate;
        this.orderServiceClient = orderServiceClient;
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    public void registerObserver(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(EntityObserver observer) {
        observers.remove(observer);
    }


    public List<User> findAll()
    {
        return userRepository.findAll();
    }

    private void enforceOwnership(Long callerUserId, String callerRole, long resourceUserId) {
        if ("ADMIN".equalsIgnoreCase(callerRole)) return;
        if (callerUserId == null || callerUserId != resourceUserId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view other user's activities");
        }
    }

//    @Cacheable(value = "user-service::user", key = "#id")
    public User findUserById(long id, Long callerUserId, String callerRole)
    {
        enforceOwnership(callerUserId, callerRole, id);
        return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User createUser(User user)
    {
        Long id=user.getId();
        String email = user.getEmail();
        String phone = user.getPhone();
        if(email!=null && userRepository.existsByEmail(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if(phone!=null && userRepository.existsByPhone(phone)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
        }

        if(id!=null && userRepository.existsById(id)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }
        if(user.getCreatedAt() == null || user.getCreatedAt().equals("") || user.getCreatedAt().equals("null"))
        {
            user.setCreatedAt(LocalDateTime.now());
        }
        User saved = userRepository.save(user);
        log.info("{} {} saved with status={}", "User", saved.getId(), saved.getStatus());

        Map<String, Object> authEvent = new HashMap<>();
        authEvent.put("userId", saved.getId());  // now ID exists
        authEvent.put("action", "USER_CREATED");

        notifyObservers("USER_CREATED", authEvent);

        return saved;
    }

    @Caching(
            put = {
                    @CachePut(value = "user-service::user", key = "#id"),
            },
            evict = {
                    @CacheEvict(value = "user-service::S1-F1", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F5", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F9", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F8", key = "#id")

            }
    )
    public User updateUser(User user, Long id, Long callerUserId, String callerRole)
    {
        enforceOwnership(callerUserId, callerRole, id);
        User updatedUser = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        updatedUser.setName(user.getName() == null ? updatedUser.getName() : user.getName());
        if(user.getEmail()!=null && userRepository.existsByEmail(user.getEmail())&& user.getEmail().equals(updatedUser.getEmail())==false){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if(user.getPhone()!=null && userRepository.existsByPhone(user.getPhone())&& user.getPhone().equals(updatedUser.getPhone())==false){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
        }
        updatedUser.setEmail(user.getEmail() == null ? updatedUser.getEmail() : user.getEmail());
        updatedUser.setPassword(user.getPassword() == null ? updatedUser.getPassword() : user.getPassword());
        updatedUser.setPhone(user.getPhone() == null ? updatedUser.getPhone() : user.getPhone());
        updatedUser.setDeliveryAddresses(user.getDeliveryAddresses() == null? updatedUser.getDeliveryAddresses(): user.getDeliveryAddresses());
        updatedUser.setPreferences(user.getPreferences() == null? updatedUser.getPreferences(): user.getPreferences());
        updatedUser.setStatus(user.getStatus() == null? updatedUser.getStatus(): user.getStatus());

        User updatedSaved = userRepository.save(updatedUser);
        log.info("{} {} saved with status={}", "User", updatedSaved.getId(), updatedSaved.getStatus());
        Map<String, Object> authEvent = new HashMap<>();
        authEvent.put("userId", updatedUser.getId());
        authEvent.put("action", "USER_UPDATED");

        notifyObservers("USER_UPDATED", authEvent);
        return updatedSaved;
    }
    @Caching(evict = {
            @CacheEvict(value = "user-service::user", key = "#id"),
            @CacheEvict(value = "user-service::S1-F1", allEntries = true),
            @CacheEvict(value = "user-service::S1-F3", allEntries = true),
            @CacheEvict(value = "user-service::S1-F5", allEntries = true),
            @CacheEvict(value = "user-service::S1-F6", allEntries = true),
            @CacheEvict(value = "user-service::S1-F8", key = "#id"),
            @CacheEvict(value = "user-service::S1-F9", allEntries = true)
    })
    public void deleteUser(Long id, Long callerUserId, String callerRole)
    {
        enforceOwnership(callerUserId, callerRole, id);
        User deletedUser = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userRepository.delete(deletedUser);

        Map<String, Object> authEvent = new HashMap<>();
        authEvent.put("userId", deletedUser.getId());
        authEvent.put("action", "USER_DELETED");
        notifyObservers("USER_DELETED", authEvent);
    }
    @Cacheable(value = "user-service::S1-F1", key = "#name + '-' + #email + '-' + #role")
    public List<User> searchUsers(String name, String email, String role)
    {
        if(name!=null && name.isEmpty())name = null;
        if(email!=null && email.isEmpty())email = null;
        if(role!=null && role.isEmpty())role = null;

        if((name==null||name.isEmpty())&&(email==null||email.isEmpty())&&(role==null||role.isEmpty())){
            return new ArrayList<>();
        }

        long start = System.currentTimeMillis();
        List<User> searchResult = userRepository.searchUsers(name, email, role);
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 1000) {
            log.warn("Slow {} took {}ms", "searchUsers", elapsed);
        }
        return searchResult;
    }



    //Service responsible for feature 1.2
    @Caching(
            put = {
                    @CachePut(value = "user-service::user", key = "#id")
            },
            evict = {
                    @CacheEvict(value = "user-service::S1-F1", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F5", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F9", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F8", key = "#id")

            }
    )
    public User updateUserPreferences(Map<String,Object> preferences, Long id){
        User updatedUser = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Map<String,Object> currentUserPreferences = updatedUser.getPreferences();
        if (currentUserPreferences == null) {
            currentUserPreferences = new HashMap<>();
        }
        if (preferences != null) {
            currentUserPreferences.putAll(preferences);
        }
        updatedUser.setPreferences(currentUserPreferences);

        User updatedSaved = userRepository.save(updatedUser);
        log.info("{} {} saved with status={}", "User", updatedSaved.getId(), updatedSaved.getStatus());
        Map<String, Object> authEvent = new HashMap<>();
        authEvent.put("userId", updatedUser.getId());
        authEvent.put("action", "USER_UPDATED");

        notifyObservers("USER_UPDATED", authEvent);
        return updatedSaved;
    }

    @Transactional
    @Caching(
            put = {
                    @CachePut(value = "user-service::user", key = "#id"),
            },
            evict = {
                    @CacheEvict(value = "user-service::S1-F1", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F5", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F9", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F8", key = "#id")

            }
    )    public ResponseStatusException deactivateUserAccount(Long id){
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        log.info("Calling {}.{} with args={}", "OrderServiceClient", "getActiveOrderCount", user.getId());
        int activeOrders;
        try {
            activeOrders = orderServiceClient.getActiveOrderCount(user.getId());
            log.info("{}.{} returned successfully", "OrderServiceClient", "getActiveOrderCount");
        } catch (Exception e) {
            log.warn("Feign call to {} failed: {}", "order-service", e.getMessage());
            throw e;
        }
        if(activeOrders > 0){
            throw new ResponseStatusException(HttpStatus.valueOf(400), "User has active orders. Cannot deactivate account.");
        }
        user.setStatus(UserStatus.DEACTIVATED);

        userRepository.save(user);
        log.info("{} {} saved with status={}", "User", user.getId(), user.getStatus());

        publisher.publishDeactivatedUser(user);

        Map<String, Object> authEvent = new HashMap<>();
        authEvent.put("userId", user.getId());
        authEvent.put("action", "USER_DEACTIVATED");

        notifyObservers("USER_DEACTIVATED", authEvent);
        return  new ResponseStatusException(HttpStatus.OK, "User account deactivated successfully");
    }

    @Cacheable(value = "user-service::S1-F6", key = "#startDate + '-' + #endDate + '-' + #limit")
    public List<TopCustomerDTO> topCustomersBySpending(LocalDate startDate, LocalDate endDate, Integer limit)
    {
        if(startDate==null || startDate.equals("") || startDate.equals("null") || startDate.isAfter(endDate))
        {
            throw new ResponseStatusException(HttpStatus.valueOf(400), "Start date cannot be after end date");
        }
        long start = System.currentTimeMillis();
        List<User> allUsers = userRepository.findAll();
        List<Object[]> result = new ArrayList<>();

        for(User user : allUsers)
        {
            Object[] row = new Object[4];
            row[0] = user.getId();
            row[1] = user.getName();
            try {
                log.info("Calling {}.{} with args={}", "CheckoutServiceClient", "getUserPaymentTotal", user.getId());
                row[2] = checkoutServiceClient.getUserPaymentTotal(user.getId(), startDate.toString(), endDate.toString());
                log.info("{}.{} returned successfully", "CheckoutServiceClient", "getUserPaymentTotal");
            } catch (Exception e) {
                log.warn("Feign call to {} failed: {}", "checkout-service", e.getMessage());
                throw e;
            }
            try {
                log.info("Calling {}.{} with args={}", "OrderServiceClient", "getTotalOrderCount", user.getId());
                row[3] = orderServiceClient.getTotalOrderCount(user.getId());
                log.info("{}.{} returned successfully", "OrderServiceClient", "getTotalOrderCount");
            } catch (Exception e) {
                log.warn("Feign call to {} failed: {}", "order-service", e.getMessage());
                throw e;
            }
            result.add(row);
        }
        result = result.stream()
                .sorted((o1,o2) ->
                        Double.compare(((Number) o2[2]).doubleValue(),((Number) o1[2]).doubleValue()))
                .limit(limit)
                .collect(Collectors.toList());
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 1000) {
            log.warn("Slow {} took {}ms", "topCustomersBySpending", elapsed);
        }
        return result.stream().map(row -> {
            Long userID = ((Number) row[0]).longValue();
            String userName = (String) row[1];
            Double totalSpent =  ((Number) row[2]).doubleValue();
            Integer orderCount = ((Number) row[3]).intValue();
            return TopCustomerDTO.builder()
                    .userId(userID)
                    .name(userName)
                    .totalSpent(totalSpent)
                    .orderCount(orderCount)
                    .build();
        }).toList();
    }
    @Cacheable(value = "user-service::S1-F5", key = "#key + '-' + #value")
    public List<User> filterUsersByPreferences(String key, String value)
    {
        if(key == null || key.isEmpty() || value == null || value.isEmpty()
                || key.equalsIgnoreCase("null") || value.equalsIgnoreCase("null")
                || value.equalsIgnoreCase("") || key.equalsIgnoreCase(""))
        {
            throw new ResponseStatusException(HttpStatus.valueOf(400), "Key/Value cannot be empty");
        }
        return userRepository.findUserByPreferencesContaining(key,value);
    }
    @Cacheable(value = "user-service::S1-F3", key = "#userId")
    public UserOrderSummaryDTO getUserOrderSummary(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        log.info("Calling {}.{} with args={}", "OrderServiceClient", "getUserOrderSummary", userId);
        OrderSummaryDTO orders;
        try {
            orders = orderServiceClient.getUserOrderSummary(userId);
            log.info("{}.{} returned successfully", "OrderServiceClient", "getUserOrderSummary");
        } catch (Exception e) {
            log.warn("Feign call to {} failed: {}", "order-service", e.getMessage());
            throw e;
        }

        return UserOrderSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalOrders(orders.totalOrders().intValue())
                .deliveredOrders(orders.deliveredOrders().intValue())
                .cancelledOrders(orders.cancelledOrders().intValue())
                .totalSpent(orders.totalSpent().doubleValue())
                .averageOrderAmount(orders.avgOrderAmount().doubleValue())
                .build();
    }

    @Cacheable(value = "user-service::S1-F9", key = "#diet + '-' + #minimumOrders")
    public List<User> findUsersByPreferencesAndMinimumOrders(String diet, Integer minimumOrders) {
        if (diet == null || diet.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.valueOf(400), "Diet cannot be null or empty");
        }
        if (minimumOrders == null || minimumOrders < 0) {
            throw new ResponseStatusException(HttpStatus.valueOf(400), "Minimum orders cannot be null or less than 0");
        }
        List<User> users = userRepository.findUserByPreferencesContaining("dietaryRestrictions", diet);
        List<User> result = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (User user : users)
        {
            log.info("Calling {}.{} with args={}", "OrderServiceClient", "getTotalOrderCount", user.getId());
            long totalOrders;
            try {
                totalOrders = orderServiceClient.getTotalOrderCount(user.getId());
                log.info("{}.{} returned successfully", "OrderServiceClient", "getTotalOrderCount");
            } catch (Exception e) {
                log.warn("Feign call to {} failed: {}", "order-service", e.getMessage());
                throw e;
            }
            if(totalOrders >= minimumOrders)
            {
                result.add(user);
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 1000) {
            log.warn("Slow {} took {}ms", "findUsersByPreferencesAndMinimumOrders", elapsed);
        }
        return result;
    }
    @Transactional
    @Caching(
            put = {
                    @CachePut(value = "user-service::user", key = "#userId"),
            },
            evict = {
                    @CacheEvict(value = "user-service::S1-F1", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F5", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F9", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F8", key = "#userId")

            }
    )
    public User setDefaultDeliveryAddress(long userId, long addressId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        DeliveryAddress address = deliveryAddressRepository.findById(addressId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        if(!user.equals(address.getUser())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address does not belong to user");
        }
        user.getDeliveryAddresses().forEach(addr -> {;
            if (addr.getId().equals(addressId)) {
                addr.setDefault(true);
                deliveryAddressRepository.save(addr);
            } else {
                addr.setDefault(false);
                deliveryAddressRepository.save(addr);
            }
        });
        Map<String, Object> authEvent = new HashMap<>();
        authEvent.put("userId", user.getId());
        authEvent.put("action", "DEFAULT_ADDRESS_SET");
        notifyObservers("DEFAULT_ADDRESS_SET", authEvent);

        return user;
    }

    public List<DeliveryAddress> getDeliveryAddressesForUser(long userId) {
        User user=userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return user.getDeliveryAddresses();
    }
    @Cacheable(value = "user-service::S1-F8", key = "#id")
    public UserProfileDTO getUserProfile(Long id) {
        User user = userRepository.findByIdWithDeliveryAddresses(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + id));

        List<DeliveryAddressDTO> addressDtos = user.getDeliveryAddresses()
                .stream()
                .map(addr -> new DeliveryAddressDTO(
                        addr.getId(),
                        addr.getLabel(),
                        addr.getStreetAddress(),
                        addr.getCity(),
                        addr.getLatitude(),
                        addr.getLongitude(),
                        addr.getDefault(),
                        addr.getMetadata(),
                        addr.getCreatedAt())).collect(Collectors.toList());


        return UserProfileDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .preferences(user.getPreferences())
                .deliveryAddresses(addressDtos)
                .totalAddresses(addressDtos.size())
                .build();
    }
    @Caching(
            evict = {
                    @CacheEvict(value = "user-service::S1-F1", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F5", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F9", allEntries = true),
                    @CacheEvict(value = "user-service::user", key = "#id"),
                    @CacheEvict(value = "user-service::S1-F8", key = "#id")
            }
    )
    public User updateUserRole(long id, UserRole role) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserRole oldRole=user.getUserRole();
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        log.info("{} {} saved with status={}", "User", updatedUser.getId(), updatedUser.getStatus());

        Map<String, Object> authEvent = new HashMap<>();
        authEvent.put("userId", user.getId());
        authEvent.put("action", "ROLE_CHANGED");
        Map<String, Object> details = new HashMap<>();
        details.put("old Role", oldRole);
        details.put("new Role", role);
        authEvent.put("details", details);
        notifyObservers("ROLE_CHANGED", authEvent);

        return updatedUser;

    }


    public void incrementUserOrderStats(Long userId, java.math.BigDecimal totalAmount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + userId));
        int newOrders   = (user.getTotalOrders() == null ? 0 : user.getTotalOrders()) + 1;
        int amountToAdd = totalAmount != null ? totalAmount.intValue() : 0;
        int newSpent    = (user.getTotalSpent()  == null ? 0 : user.getTotalSpent())  + amountToAdd;
        user.setTotalOrders(newOrders);
        user.setTotalSpent(newSpent);
        userRepository.save(user);
    }

    public void decrementUserOrderStats(Long userId, Long orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + userId));
        java.math.BigDecimal orderAmount = java.math.BigDecimal.ZERO;
        log.info("Calling {}.{} with args={}", "OrderServiceClient", "getOrder", orderId);
        try {
            com.team05.fooddelivery.contracts.dto.OrderDTO order = orderServiceClient.getOrder(orderId);
            log.info("{}.{} returned successfully", "OrderServiceClient", "getOrder");
            if (order != null && order.totalAmount() != null) {
                orderAmount = order.totalAmount();
            }
        } catch (Exception e) {
            log.warn("Feign call to {} failed: {}", "order-service", e.getMessage());
        }
        int newOrders = Math.max(0, (user.getTotalOrders() == null ? 0 : user.getTotalOrders()) - 1);
        int newSpent  = Math.max(0, (user.getTotalSpent()  == null ? 0 : user.getTotalSpent())  - orderAmount.intValue());
        user.setTotalOrders(newOrders);
        user.setTotalSpent(newSpent);
        userRepository.save(user);
    }

    @Cacheable(value = "user-service::S1-F12", key = "#id")
    public ActivityFeedDTO getUserActivityFeed(long id, int page, int size, Long callerUserId, String callerRole) {
        enforceOwnership(callerUserId, callerRole, id);

        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (size == 0) {
            size = 10;
        }

        if (size > 100) {
            size = 100;
        }


        long documentsToSkip = page * size;

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").eq(id)),
                Aggregation.sort(Sort.Direction.DESC, "timestamp"),
                Aggregation.skip(documentsToSkip),
                Aggregation.limit(size)
        );

        long start = System.currentTimeMillis();
        List<AuthEvent> result = mongoTemplate.aggregate(aggregation, "auth_events", AuthEvent.class).getMappedResults();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 1000) {
            log.warn("Slow {} took {}ms", "getUserActivityFeed", elapsed);
        }

        ActivityFeedDTO feed = new ActivityFeedDTO(result, page, size, result.size());

        return feed;
    }


}