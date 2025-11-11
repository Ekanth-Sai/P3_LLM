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

    public User createUser(String email,String firstName,String lastName, String rawPassword,String project,String department,String role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));  // Hashing the password here
        user.setRole(role);
        user.setStatus("PENDING");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProject(project);
        user.setDepartment(department);

        return userRepository.save(user);
    }

}
