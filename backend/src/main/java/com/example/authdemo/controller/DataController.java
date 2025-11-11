package com.example.authdemo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authdemo.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "http://localhost:4200")
public class DataController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/departments")
    public List<String> getDepartments() {
        return userRepository.findAll().stream()
                .map(user -> user.getDepartment())
                .filter(department -> department != null && !department.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @GetMapping("/projects")
    public List<String> getProjects() {
        return userRepository.findAll().stream()
                .map(user -> user.getProject())
                .filter(project -> project != null && !project.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @GetMapping("/roles")
    public List<String> getRoles() {
        return userRepository.findAll().stream()
                .map(user -> user.getRole())
                .filter(role -> role != null && !role.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
