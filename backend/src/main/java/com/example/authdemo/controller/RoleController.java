package com.example.authdemo.controller;

import com.example.authdemo.model.Role;
import com.example.authdemo.model.SubRole;
import com.example.authdemo.service.RBACService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "http://localhost:4200")
public class RoleController {
    private final RBACService rbacService;

    public RoleController(RBACService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<List<Map<String, Object>>> getRoleHierarchy() {
        try {
            List<Map<String, Object>> hierarchy = rbacService.getRoleHierarchyTree();
            return ResponseEntity.ok(hierarchy);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        try {
            List<Role> roles = rbacService.getAllRoles();
            List<Map<String, Object>> roleList = new ArrayList<>();

            for (Role role : roles) {
                Map<String, Object> roleMap = new HashMap<>();
                roleMap.put("id", role.getId());
                roleMap.put("roleName", role.getRoleName());
                roleMap.put("displayName", role.getDisplayName());
                roleMap.put("description", role.getDescription());
                roleMap.put("allowedSensitivity", role.getAllowedSensitivity());
                roleMap.put("parentRoleId", role.getParentRole() != null ? role.getParentRole().getId() : null);
                roleMap.put("parentRoleName", role.getParentRole() != null ? role.getParentRole().getRoleName() : null);

                roleList.add(roleMap);
            }
            return ResponseEntity.ok(roleList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{roleId}/subroles")
    public ResponseEntity<List<Map<String, Object>>> getSubRoles(@PathVariable Long roleId) {
        try {
            List<SubRole> subRoles = rbacService.getSubRolesForRole(roleId);
            List<Map<String, Object>> subRoleList = new ArrayList<>();

            for (SubRole subRole : subRoles) {
                Map<String, Object> subRoleMap = new HashMap<>();
                subRoleMap.put("id", subRole.getId());
                subRoleMap.put("subRoleName", subRole.getSubRoleName());
                subRoleMap.put("displayName", subRole.getDisplayName());
                subRoleMap.put("description", subRole.getDescription());
                subRoleMap.put("allowedSensitivity", subRole.getAllowedSensitivity());
                subRoleList.add(subRoleMap);
            }

            return ResponseEntity.ok(subRoleList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/subroles/all")
    public ResponseEntity<List<Map<String, Object>>> getAllSubRoles() {
        try {
            List<SubRole> subRoles = rbacService.getAllSubRoles();
            List<Map<String, Object>> subRoleList = new ArrayList<>();

            for (SubRole subRole : subRoles) {
                Map<String, Object> subRoleMap = new HashMap<>();
                subRoleMap.put("id", subRole.getId());
                subRoleMap.put("subRoleName", subRole.getSubRoleName());
                subRoleMap.put("displayName", subRole.getDisplayName());
                subRoleMap.put("description", subRole.getDescription());
                subRoleMap.put("allowedSensitivity", subRole.getAllowedSensitivity());
                subRoleMap.put("parentRoleId", subRole.getParentRole().getId());
                subRoleMap.put("parentRoleName", subRole.getParentRole().getRoleName());
                subRoleList.add(subRoleMap);
            }

            return ResponseEntity.ok(subRoleList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/expand")
    public ResponseEntity<Map<String, Object>> expandRoles(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> selectedRoles = request.get("roles");

            if (selectedRoles == null || selectedRoles.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No roles provided"));
            }

            List<String> expandedRoles = rbacService.expandRolesToIncludeParents(selectedRoles);

            Map<String, Object> response = new HashMap<>();
            response.put("selectedRoles", selectedRoles);
            response.put("expandedRoles", expandedRoles);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to expand roles: " + e.getMessage()));
        }
    }
    
    @GetMapping("/users/{email}/permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions(@PathVariable String email) {
        try {
            Map<String, Object> filters = rbacService.getUserFilters(email);
            
            return ResponseEntity.ok(filters);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get permission: " + e.getMessage()));
        }
    }
}
