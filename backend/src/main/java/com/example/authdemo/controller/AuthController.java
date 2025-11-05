package com.example.authdemo.controller;

import com.example.authdemo.dto.LoginRequest;
import com.example.authdemo.model.User;
import com.example.authdemo.repository.UserRepository;
import com.example.authdemo.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;

//    @PostMapping("/login")
//    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
//
//        // ️Find user by email
//        Optional<User> optionalUser = userRepository.findByEmail(loginRequest.getEmail());
//        if (optionalUser.isEmpty()) {
//            return ResponseEntity.status(401).body(Map.of(
//                    "status", "error",
//                    "message", "Invalid credentials"
//            ));
//        }
//
//        User user = optionalUser.get();
//
//        // Verify password using BCrypt
//        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
//            return ResponseEntity.status(401).body(Map.of(
//                    "status", "error",
//                    "message", "Invalid credentials"
//            ));
//        }
//
//        // Check account status
//        if ("PENDING".equals(user.getStatus())) {
//            return ResponseEntity.status(403).body(Map.of(
//                    "status", "pending",
//                    "message", "Your account is awaiting admin approval"
//            ));
//        }
//        if ("DECLINED".equals(user.getStatus())) {
//            return ResponseEntity.status(403).body(Map.of(
//                    "status", "declined",
//                    "message", "Your account has been declined"
//            ));
//        }
//
//        //  Return role and success message
//        return ResponseEntity.ok(Map.of(
//                "status", "success",
//                "role", user.getRole(),
//                "email", user.getEmail()
//        ));
//    }
    

@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
    return userRepository.findByEmail(loginRequest.getEmail())
        .map(user -> {
            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {

                //  Check account status
                if ("PENDING".equals(user.getStatus())) {
                    return ResponseEntity.ok(Map.of("status", "pending", "message", "Your account is awaiting admin approval"));
                }
                if ("DECLINED".equals(user.getStatus())) {
                    return ResponseEntity.ok(Map.of("status", "declined", "message", "Your account has been declined"));
                }

                //  Generate JWT token
                String token = jwtUtil.generateToken(user.getEmail());
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "role", user.getRole(),
                    "token", token
                ));
            } else {
                return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Invalid credentials"
                ));
            }
        })
        .orElseGet(() -> ResponseEntity.status(401).body(Map.of(
            "status", "error",
            "message", "Invalid credentials"
        )));
}

    @GetMapping("/me")
    public Map<String, String> getLoggedInUser(Principal principal) {
        if (principal != null) {
            return Map.of("username", principal.getName());
        } else {
            return Map.of("username", "Guest");
        }
    }
}


