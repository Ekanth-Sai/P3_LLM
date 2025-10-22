package com.example.authdemo.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.authdemo.model.User;
import com.example.authdemo.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
//    private String firstName;
//    private String lastName;
//    private String email;
//    private String password;
//    private String role;
//
//    private String status;
//
//    private String project;
//    private String designation;
//    private String manager;

    public User createUser(String email,String firstName,String lastName, String rawPassword,String project,String designation,String manager) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));  // Hashing the password here
        user.setRole("ADMIN");
        user.setStatus("ACTIVE");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDesignation(designation);
        user.setProject(project);
        user.setManager(manager);

        return userRepository.save(user);
    }

}
