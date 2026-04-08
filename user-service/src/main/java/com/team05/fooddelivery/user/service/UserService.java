package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
        return userRepository.save(user);
    }

    public User updateUser(User user, Long id)
    {
        User updatedUser = userRepository.findById(id).get();
        updatedUser.setName(user.getName());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setPassword(user.getPassword());
        updatedUser.setPhone(user.getPhone());
        return userRepository.save(updatedUser);
    }

    public User deleteUser(Long id)
    {
        User deletedUser = userRepository.findById(id).get();
        userRepository.delete(deletedUser);
        return deletedUser;
    }
}
