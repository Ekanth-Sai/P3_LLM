package com.example.authdemo.service;

import com.example.authdemo.model.User;
import com.example.authdemo.model.SubRole;
import com.example.authdemo.model.Role;
import com.example.authdemo.repository.UserRepository;
import com.example.authdemo.repository.RoleRepository;
import com.example.authdemo.repository.SubRoleRepository;

// import org.springframework.security.access.method.P;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RBACService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SubRoleRepository subRoleRepository;

    public RBACService(UserRepository userRepository, RoleRepository roleRepository, SubRoleRepository subRoleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.subRoleRepository = subRoleRepository;
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

        List<String> effectiveRoles = new ArrayList<>();
        if (user.getUserRole() != null) {
            effectiveRoles.addAll(getAllParentRoles(user.getUserRole()));
        } else {
            effectiveRoles.add(user.getEffectiveRoleName());
        }
        filters.put("roles", effectiveRoles);

        if (user.getSubRole() != null) {
            filters.put("subrole", user.getSubRole().getSubRoleName());
        }

        List<String> allowedSensitivity = getInheritedSensitivity(user);
        filters.put("sensitivity", allowedSensitivity);

        // if (allowedSensitivity.isEmpty()) {
        //     allowedSensitivity = getDefaultSensitivity(user.getRole());
        // }
        // filters.put("sensitivity", allowedSensitivity);

        return filters;
    }

    public List<String> getAllParentRoles(Role role) {
        List<String> roles = new ArrayList<>();
        Role current = role;

        while (current != null) {
            roles.add(current.getRoleName());
            current = current.getParentRole();
        }

        return roles;
    }

    public List<String> getAllChildRoles(Role role) {
        List<String> roles = new ArrayList<>();
        collectChileRoles(role, roles);

        return roles;
    }

    private void collectChildRoles(Role role, List<String> collector) {
        if (role == null) {
            return;
        }
        collector.add(role.getRoleName());

        List<Role> children = roleRepository.findByParentRole(role);
        for (Role child : children) {
            collectChildRoles(child, collector);
        }
    }

    private List<String> getInheritedSensitivity(User user) {
        Set<String> sensitivity = new HashSet<>();

        sensitivity.addAll(user.getAllowedSensitivityList());

        if (user.getUserRole() != null) {
            List<String> parentRoles = getAllParentRoles(user.getUserRole());

            for (String roleName : parentRoles) {
                roleRepository.findByRoleName(roleName).ifPresent(r -> {
                    sensitivity.addAll(Arrays.asList(r.getAllowedSensitivity()));
                });
            }
        }

        if(user.getSubRole() != null) {
            sensitivity.addAll(Arrays.asList(user.getSubRole().getAllowedSensitivity()));
        }

        if (sensitivity.isEmpty()) {
            sensitivity.addAll(getDefaultSensitivity(user.getEffectiveRoleName()));
        }

        return new ArrayList<>(sensitivity);
    }

    private List<String> getDefaultSensitivity(String roleName) {
        if (roleName == null) {
            return Arrays.asList("Public");
        }

        return switch (roleName.toUpperCase()) {
            case "CEO", "CTO", "CPO", "CFO" ->
                Arrays.asList("Public", "Internal", "Confidential", "Highly Confidential");
            case "HR", "PRODUCT_MANAGER", "PROJECT_MANAGER", "TEAM_LEAD" ->
                Arrays.asList("Public", "Internal", "Confidential");
            case "SENIOR_DEVELOPER", "DEVELOPER" ->
                Arrays.asList("Public", "Internal");
            case "JUNIOR_DEVELOPER", "INTERN" ->
                Arrays.asList("Public");
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

    public boolean hasSensitivityAccess(String email, String sensitivityLevel) {
        Map<String, Object> filters = getUserFilters(email);

        @SuppressWarnings("unchecked")
        List<String> allowedSensitivity = (List<String>) filters.get("sensitivity");

        return allowedSensitivity != null
                && allowedSensitivity.stream().anyMatch(s -> s.equalsIgnoreCase(sensitivityLevel));
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

    public List<String> expandRolesToIncludeParents(List<String> roleNames) {
        Set<String> expandedRoles = new HashSet<>();

        for (String roleName : roleNames) {
            roleRepository.findByRoleName(roleName).ifPresent(role -> {
                expandedRoles.addAll(getAllParentRoles(role));
            });
        }

        return new ArrayList<>(expandedRoles);
    }

    public List<Map<String, Object>> getRoleHierarchyTree() {
        List<Role> roots = roleRepository.findByParentRoleIsNull();

        return roots.stream().map(this::buildRoleNode).collect(Collectors.toList());
    }
    
    private Map<String, Object> buildRoleNode(Role role) {
        Map<String, Object> node = new HashMap<>();

        node.put("id", role.getId());
        node.put("name", role.getRoleName());
        node.put("displayName", role.getDisplayName());
        node.put("description", role.getDescription());
        node.put("allowedSensitivity", role.getAllowedSensitivity());

        List<Role> children = roleRepository.findByParentRole(role);

        if (!children.isEmpty()) {
            node.put("children", children.stream().map(this::buildRoleNode).collect(Collectors.toList()));
        }

        return node;
    }

    public List<SubRole> getSubRolesForRole(Long roleId) {
        return subRoleRepository.findByParentRoleId(roleId);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public List<SubRole> getAllSubRoles() {
        return subRoleRepository.findAll();
    }
}
