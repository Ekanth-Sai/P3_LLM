package com.example.authdemo.controller;

import com.example.authdemo.dto.LoginRequest;
import com.example.authdemo.model.User;
import com.example.authdemo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Login endpoint
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest loginRequest) {
        return userRepository.findByEmailAndPassword(loginRequest.getEmail(), loginRequest.getPassword())
                .map(user -> {
                    if (user.isPending()) {
                        return Map.of("status", "pending", "message", "Your account is awaiting admin approval");
                    }
                    return Map.of("status", "success", "role", user.getRole());
                })
                .orElse(Map.of("status", "error", "message", "Invalid credentials"));
    }

    // Signup endpoint
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);  // Hash password in production
        user.setRole("USER"); // Default role
        user.setPending(true); // Mark new user as pending
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("status", "pending", "message", "Signup successful, awaiting admin approval"));
    }
}
