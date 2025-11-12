package com.example.authdemo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.authdemo.model.User;
import com.example.authdemo.repository.UserFileRepository;
import com.example.authdemo.repository.UserRepository; 

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class SignupController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFileRepository userFileRepository;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> payload) {
        User user = new User();
        user.setFirstName(payload.get("firstName"));
        user.setLastName(payload.get("lastName"));
        user.setEmail(payload.get("email"));
        user.setPassword(payload.get("password")); // <-- add this
        user.setRole(payload.get("role"));
        user.setStatus("PENDING");
        user.setProject(payload.get("project"));
        user.setDepartment(payload.get("department"));

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("status", "pending"));
    }
    
    @GetMapping("/signup/departments")
    public ResponseEntity<List<String>> getDepartments() {
        try {
            List<String> departments = userFileRepository.findDistinctDepartments();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
    
    @GetMapping("/signup/projects")
    public ResponseEntity<List<String>> getAllProjects() {
        try {
            List<String> projects = userFileRepository.findDistinctProjects();
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    @GetMapping("/signup/projects/{department}")
    public ResponseEntity<List<String>> getProjectsByDepartment(@PathVariable String department) {
        try {
            List<String> projects = userFileRepository.findDistinctProjectsByDepartment(department);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
}
