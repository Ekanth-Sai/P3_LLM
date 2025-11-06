package com.example.authdemo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authdemo.model.User; 
import com.example.authdemo.repository.UserRepository;
import com.example.authdemo.service.UserFileService;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class SignupController {

    @Autowired
    private UserRepository userRepository;
    

    @PostMapping("/")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> payload) {
        User user = new User();
        user.setFirstName(payload.get("firstName"));
        user.setLastName(payload.get("lastName"));
        user.setEmail(payload.get("email"));
        user.setPassword(payload.get("password")); // <-- add this
        user.setRole("USER");
        user.setStatus("PENDING");
        user.setProject(payload.get("project"));
        user.setDesignation(payload.get("designation"));
        user.setManager(payload.get("manager"));

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("status", "pending"));
    }
    

    
    

}
