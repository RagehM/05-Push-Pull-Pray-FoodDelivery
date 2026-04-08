package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.enums.UserStatus;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll()
    {
        return userRepository.findAll();
    }

    public User findUserById(long id)
    {
        return userRepository.findById(id).orElse(null);
    }

    public User createUser(User user)
    {
        if(user.getCreatedAt() == null || user.getCreatedAt().equals("") || user.getCreatedAt().equals("null"))
        {
            user.setCreatedAt(LocalDateTime.now());
        }
        return userRepository.save(user);
    }

    public User updateUser(User user, Long id)
    {
        User updatedUser = userRepository.findById(id).get();
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
        User deletedUser = userRepository.findById(id).get();
        userRepository.delete(deletedUser);
        return deletedUser;
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
}
