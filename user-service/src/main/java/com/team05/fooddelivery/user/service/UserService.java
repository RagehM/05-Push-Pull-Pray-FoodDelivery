package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.dto.*;
import com.team05.fooddelivery.user.dto.TopCustomerDTO;
import com.team05.fooddelivery.user.enums.UserRole;
import com.team05.fooddelivery.user.enums.UserStatus;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.repository.Aggregation.*;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final AuthEventRepository authEventRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public UserService(UserRepository userRepository, DeliveryAddressRepository deliveryAddressRepository, AuthEventRepository authEventRepository, MongoTemplate mongoTemplate) {
        this.userRepository = userRepository;
        this.deliveryAddressRepository=deliveryAddressRepository;
        this.authEventRepository = authEventRepository;
        this.observers.add(
                new MongoEventLogger<>(this.authEventRepository, MongoEvent.EventType.AUTH)
        );
        this.mongoTemplate = mongoTemplate;
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

    @Cacheable(value = "user-service::user", key = "#id")
    public User findUserById(long id)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> authenticatedUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            authenticatedUser = userRepository.findByEmail(authentication.getName());
        }
        if (authenticatedUser.get().getUserRole() == UserRole.CUSTOMER && id != authenticatedUser.get().getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view other user's activities");
        }
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
    public User updateUser(User user, Long id)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> authenticatedUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            authenticatedUser = userRepository.findByEmail(authentication.getName());
        }
        if (authenticatedUser.get().getUserRole() == UserRole.CUSTOMER && id != authenticatedUser.get().getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view other user's activities");
        }
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
    public void deleteUser(Long id)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> authenticatedUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            authenticatedUser = userRepository.findByEmail(authentication.getName());
        }
        if (authenticatedUser.get().getUserRole() == UserRole.CUSTOMER && id != authenticatedUser.get().getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view other user's activities");
        }
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

        return userRepository.searchUsers(name, email, role);
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
        currentUserPreferences.putAll(preferences);
        updatedUser.setPreferences(currentUserPreferences);

        User updatedSaved = userRepository.save(updatedUser);
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
        List<Object> activeOrders=userRepository.findOrdersByUserId(id);
        if(activeOrders.size()>0){
            throw new ResponseStatusException(HttpStatus.valueOf(400), "User has active orders. Cannot deactivate account.");
        }
        user.setStatus(UserStatus.DEACTIVATED);

        userRepository.save(user);

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
        List<Object[]> result =  userRepository.findUsersWithHighestSpent(limit,startDate,endDate);
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
        List<Object[]> orders = userRepository.findTotalOrders(userId);
        List<Object[]> deliveredOrders = userRepository.findDeliveredOrders(userId);
        List<Object[]> cancelledOrders = userRepository.findCancelledOrders(userId);

        Double totalSpent;
        if(!deliveredOrders.isEmpty()){
            totalSpent = deliveredOrders.stream()
                    .map(order -> ((Number) order[5]).doubleValue())  // total_amount is at index 5
                    .reduce(0.0, Double::sum);
        } else {
            totalSpent = 0.0;
        }

        Double averageOrderAmount = !orders.isEmpty() ? totalSpent / deliveredOrders.size() : 0.0;



        return UserOrderSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalOrders(orders.size())
                .deliveredOrders(deliveredOrders.size())
                .cancelledOrders(cancelledOrders.size())
                .totalSpent(totalSpent)
                .averageOrderAmount(averageOrderAmount)
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
        List<Long> result = userRepository.findUsersByDietaryPreferenceAndMinimumOrders(diet, minimumOrders);
        List<User> users = new ArrayList<>();

        result.forEach(id -> users.add(userRepository.findById(id).orElseThrow()));
        return users;
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

    @Cacheable(value = "user-service::S1-F12", key = "#id")
    public ActivityFeedDTO getUserActivityFeed(long id, int page, int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> authenticatedUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            authenticatedUser = userRepository.findByEmail(authentication.getName());
        }


        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (authenticatedUser.get().getUserRole() == UserRole.CUSTOMER && id != authenticatedUser.get().getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view other user's activities");
        }

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

        List<AuthEvent> result = mongoTemplate.aggregate(aggregation, "auth_events", AuthEvent.class).getMappedResults();

        ActivityFeedDTO feed = new ActivityFeedDTO(result, page, size, result.size());

        return feed;
    }
}