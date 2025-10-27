package com.example.authdemo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authdemo.model.User;
import com.example.authdemo.security.UserService;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        User newUser = userService.createUser(user.getEmail(),user.getFirstName(),
		        		user.getLastName(), user.getPassword(),user.getProject(),user.getDepartment(),
		        		user.getDesignation(),user.getManager());
        return ResponseEntity.ok(Map.of(
        	    "status", "success",
        	    "message", "User created with ID: " + newUser.getId()
        	));
    }
}