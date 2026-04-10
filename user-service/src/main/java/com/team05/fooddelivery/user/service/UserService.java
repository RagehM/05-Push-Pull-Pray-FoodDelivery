package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.dto.TopCustomerDTO;
import com.team05.fooddelivery.user.dto.TopCustomerDTO;
import com.team05.fooddelivery.user.dto.UserOrderSummaryDTO;
import com.team05.fooddelivery.user.enums.UserRole;
import com.team05.fooddelivery.user.enums.UserStatus;
import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.DeliveryAddressRepository;
import com.team05.fooddelivery.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class UserService {
    private final UserRepository userRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;

    @Autowired
    public UserService(UserRepository userRepository, DeliveryAddressRepository deliveryAddressRepository) {
        this.userRepository = userRepository;
        this.deliveryAddressRepository=deliveryAddressRepository;
    }

    public List<User> findAll()
    {
        return userRepository.findAll();
    }

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
        return userRepository.save(user);
    }

    public User updateUser(User user, Long id)
    {
        User updatedUser = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        updatedUser.setName(user.getName() == null ? updatedUser.getName() : user.getName());
        updatedUser.setEmail(user.getEmail() == null ? updatedUser.getEmail() : user.getEmail());
        updatedUser.setPassword(user.getPassword() == null ? updatedUser.getPassword() : user.getPassword());
        updatedUser.setPhone(user.getPhone() == null ? updatedUser.getPhone() : user.getPhone());
        updatedUser.setDeliveryAddresses(user.getDeliveryAddresses() == null? updatedUser.getDeliveryAddresses(): user.getDeliveryAddresses());
        updatedUser.setPreferences(user.getPreferences() == null? updatedUser.getPreferences(): user.getPreferences());
        updatedUser.setStatus(user.getStatus() == null? updatedUser.getStatus(): user.getStatus());
        return userRepository.save(updatedUser);
    }

    public User deleteUser(Long id)
    {
        User deletedUser = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userRepository.delete(deletedUser);
        return deletedUser;
    }

    public List<User> searchUsers(String name, String email, String role)
    {
        if(name!=null && name.isEmpty())name = null;
        if(email!=null && email.isEmpty())email = null;
        if(role!=null && role.isEmpty())role = null;


        return userRepository.searchUsers(name, email, role);
    }


    //Service responsible for feature 1.2
    public User updateUserPreferences(Map<String,Object> preferences, Long id){
        User updatedUser = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Map<String,Object> currentUserPreferences = updatedUser.getPreferences();
        currentUserPreferences.putAll(preferences);
        updatedUser.setPreferences(currentUserPreferences);
        return userRepository.save(updatedUser);
    }
  
    @Transactional
    public ResponseStatusException deactivateUserAccount(Long id){
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        List<Object> activeOrders=userRepository.findOrdersByUserId(id);
        if(activeOrders.size()>0){
            throw new ResponseStatusException(HttpStatus.valueOf(400), "User has active orders. Cannot deactivate account.");
        }
        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(user);
        return  new ResponseStatusException(HttpStatus.OK, "User account deactivated successfully");
    }


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

    public List<User> filterUsersByPreferences(String key, String value)
    {
        if(key == null || key.isEmpty() || value == null || value.isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.valueOf(400), "User has active orders. Cannot deactivate account.");
        }
        return userRepository.findUserByPreferencesContaining(key,value);
    }
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


    public List<User> findUsersByPreferencesAndMinimumOrders(String diet, Integer minimumOrders)
    {
        if(diet == null || diet.isEmpty() || minimumOrders == null || minimumOrders < 0 || diet.equalsIgnoreCase("null"))
        {
            throw new ResponseStatusException(HttpStatus.valueOf(400), "Diet or minimum orders cannot be null or empty");
        }
        List<Long> result = userRepository.findUsersByDietaryPreferenceAndMinimumOrders(diet,minimumOrders);
        List<User> users = new ArrayList<>();

        result.forEach(id -> users.add(userRepository.findById(id).orElseThrow()));
        return users;
    }
    @Transactional
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

        return user;
    }
}
