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

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest loginRequest) {
        return userRepository.findByEmailAndPassword(loginRequest.getEmail(), loginRequest.getPassword())
                .map(user -> {
                    if ("PENDING".equals(user.getStatus())) {
                        return Map.of("status", "pending", "message", "Your account is awaiting admin approval");
                    }
                    if ("DECLINED".equals(user.getStatus())) {
                        return Map.of("status", "declined", "message", "Your account has been declined");
                    }
                    return Map.of("status", "success", "role", user.getRole());
                })
                .orElse(Map.of("status", "error", "message", "Invalid credentials"));
    }
}
