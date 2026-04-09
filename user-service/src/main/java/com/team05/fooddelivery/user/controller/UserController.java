package com.team05.fooddelivery.user.controller;

import com.team05.fooddelivery.user.dto.UserOrderSummaryDTO;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.UserRepository;
import com.team05.fooddelivery.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService)
    {
        this.userService = userService;
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

    @GetMapping("/{id}/order-summary")
    public UserOrderSummaryDTO getUserOrderSummary(@PathVariable long id){
        return userService.getUserOrderSummary(id);
    }

}
