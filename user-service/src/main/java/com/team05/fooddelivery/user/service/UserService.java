package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.dto.*;
import com.team05.fooddelivery.user.dto.TopCustomerDTO;
import com.team05.fooddelivery.user.enums.UserRole;
import com.team05.fooddelivery.user.enums.UserStatus;
import com.team05.fooddelivery.user.factory.AuthEventFactory;
import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.DeliveryAddressRepository;
import com.team05.fooddelivery.user.repository.UserRepository;
import com.team05.fooddelivery.user.repository.mongo.AuthEventRepository;
import com.team05.shared.model.mongo.MongoEvent;
import com.team05.shared.observer.EntityObserver;
import com.team05.shared.observer.MongoEventLogger;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
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

@Service
public class UserService {
    private final UserRepository userRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final AuthEventRepository authEventRepository;
    private final AuthEventFactory authEventFactory = new AuthEventFactory();

    @Autowired
    public UserService(UserRepository userRepository, DeliveryAddressRepository deliveryAddressRepository,AuthEventRepository authEventRepository) {
        this.userRepository = userRepository;
        this.deliveryAddressRepository=deliveryAddressRepository;
        this.authEventRepository = authEventRepository;
        this.observers.add(
                new MongoEventLogger<>(this.authEventRepository, MongoEvent.EventType.AUTH, authEventFactory)
        );
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }


    public List<User> findAll()
    {
        return userRepository.findAll();
    }

@Cacheable(value = "user-service::user", key = "#id")
    public User findUserById(long id)
    {
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
                    @CachePut(value = "user-service::S1-F8", key = "#id")
            },
            evict = {
                    @CacheEvict(value = "user-service::S1-F1", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F5", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F9", allEntries = true)
            }
    )
    public User updateUser(User user, Long id)
    {
        User updatedUser = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        updatedUser.setName(user.getName() == null ? updatedUser.getName() : user.getName());
        if(user.getEmail()!=null && userRepository.existsByEmail(user.getEmail())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if(user.getPhone()!=null && userRepository.existsByPhone(user.getPhone())){
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one search parameter must be provided");
        }

        return userRepository.searchUsers(name, email, role);
    }



    //Service responsible for feature 1.2
    @Caching(
            put = {
                    @CachePut(value = "user-service::S1-F8", key = "#id"),
                    @CachePut(value = "user-service::user", key = "#id")
            },
            evict = {
                    @CacheEvict(value = "user-service::S1-F1", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F5", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F9", allEntries = true)
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
                    @CachePut(value = "user-service::S1-F8", key = "#id")
            },
            evict = {
                    @CacheEvict(value = "user-service::S1-F1", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F5", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F9", allEntries = true)
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
        List<TopCustomerDTO> topCustomerDTOs = new ArrayList<TopCustomerDTO>();
        result.forEach(object -> {
            Long userID = (Long) object[0];
            String userName = (String) object[1];
            Double totalSpent = (Double) object[2];
            Integer orderCount = Math.toIntExact((Long) object[3]);
            topCustomerDTOs.add(new TopCustomerDTO(userID,userName, totalSpent, orderCount ));
        });
        return topCustomerDTOs;
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



        return new UserOrderSummaryDTO(
                user.getId(),
                user.getName(),
                orders.size(),
                deliveredOrders.size(),
                cancelledOrders.size(),
                totalSpent,
                averageOrderAmount
        );
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
                    @CachePut(value = "user-service::user", key = "#id"),
                    @CachePut(value = "user-service::S1-F8", key = "#id")
            },
            evict = {
                    @CacheEvict(value = "user-service::S1-F1", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F5", allEntries = true),
                    @CacheEvict(value = "user-service::S1-F9", allEntries = true)
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

        return new UserProfileDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getPreferences(),
                addressDtos,
                addressDtos.size());
    }
}