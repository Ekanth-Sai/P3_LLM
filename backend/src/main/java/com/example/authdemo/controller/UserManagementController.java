package com.example.authdemo.controller;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authdemo.model.User;
import com.example.authdemo.security.UserService;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(@RequestParam String email,
                                        @RequestParam String password,
                                        @RequestParam String role) {
        User newUser = userService.createUser(email, password, role);
        return ResponseEntity.ok("User created with ID: " + newUser.getId());
    }
}