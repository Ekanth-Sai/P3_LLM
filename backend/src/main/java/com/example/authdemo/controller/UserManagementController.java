package com.example.authdemo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authdemo.model.User;
import com.example.authdemo.security.UserService;
import com.example.authdemo.service.UserFileService;

@RestController
//@RequestMapping("/signup")
@CrossOrigin(origins = "http://localhost:4200")
public class UserManagementController {

    private final UserService userService;
    private UserFileService userFileService;

    public UserManagementController(UserService userService,UserFileService userFileService) {
        this.userService = userService;
        this.userFileService = userFileService;
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
    
    @GetMapping("/departments")
    public ResponseEntity<List<String>> getAllDepartments() {
        List<String> departments = userFileService.getDepartments();
        if (departments.isEmpty()) {
            return ResponseEntity.noContent().build(); 
        }
        return ResponseEntity.ok(departments); 
    }
    
    @GetMapping("/projects/{department}")
    public ResponseEntity<List<String>> getProjectsByDepartment(@PathVariable String department) {
        List<String> projects = userFileService.getProjectByDepartment(department);
        if (projects.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(projects);
    }
}