package com.example.authdemo.service;

import com.example.authdemo.model.User;
import com.example.authdemo.repository.UserRepository;

// import org.springframework.security.access.method.P;
// import org.springframework.stereotype.Service;
import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class RBACService {
    private final UserRepository userRepository;

    public RBACService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<String, Object> getUserFilters(String email) {
        Optional<User> optUser = userRepository.findByEmail(email);

        if (optUser.isEmpty()) {
            return Collections.emptyMap();
        }

        User user = optUser.get();
        Map<String, Object> filters = new HashMap<>();

        String department = user.getDepartment();
        filters.put("department", department != null ? department : "General");

        List<String> allowedProjects = user.getAllowedProjectsList();
        if (allowedProjects.isEmpty()) {
            allowedProjects = Arrays.asList(user.getProject() != null ? user.getProject() : "General");
        }
        filters.put("projects", allowedProjects);

        List<String> allowedSensitivity = user.getAllowedSensitivityList();
        if (allowedSensitivity.isEmpty()) {
            allowedSensitivity = getDefaultSensitivity(user.getRole());
        }
        filters.put("sensitivity", allowedSensitivity);

        return filters;
    }

    private List<String> getDefaultSensitivity(String role) {
        if (role == null) {
            return Arrays.asList("Public");
        }

        return switch (role.toUpperCase()) {
            case "ADMIN" -> Arrays.asList("Public", "Internal", "Confidential", "Highly Confidential");
            case "MANAGER" -> Arrays.asList("Public", "Internal", "Confidential");
            case "USER" -> Arrays.asList("Public", "Internal");
            default -> Arrays.asList("Public");
        };
    }
    
    public boolean hasProjectAccess(String email, String projectName) {
        Map<String, Object> filters = getUserFilters(email);

        @SuppressWarnings("unchecked")
        List<String> allowedProjects = (List<String>) filters.get("projects");
        return allowedProjects != null && allowedProjects.contains(projectName);
    }

    public boolean hasDepartmentAccess(String email, String departmentName) {
        Map<String, Object> filters = getUserFilters(email);
        String userDepartment = (String) filters.get("department");
        return userDepartment != null && userDepartment.equalsIgnoreCase(departmentName);
    }

    public void updateAllowedProjects(String email, List<String> projects) {
        Optional<User> optUser = userRepository.findByEmail(email);

        if (optUser.isPresent()) {
            User user = optUser.get();
            user.setAllowedProjects(projects.toArray(new String[0]));
            userRepository.save(user);
        }
    }

    public void updateAllowedSensitivity(String email, List<String> sensitivity) {
        Optional<User> optUser = userRepository.findByEmail(email);

        if (optUser.isPresent()) {
            User user = optUser.get();
            user.setAllowedSensitivity(sensitivity.toArray(new String[0]));
            userRepository.save(user);
        }
    }
}
