package com.example.authdemo.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String role;

    private String status;

    private String project;
    private String department;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role userRole;

    @ManyToOne
    @JoinColumn(name = "sub_role_id")
    private SubRole subRole;

    @Column(columnDefinition = "text[]")
    private String[] allowedProjects;

    @Column(columnDefinition = "text[]")
    private String[] allowedSensitivity;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }



    public String getDepartment() {
    	return department;
    }

    public void setDepartment(String department) {
    	this.department = department;
    }

    public Role getUserRole() {
        return userRole;
    }

    public void setUserRole(Role userRole) {
        this.userRole = userRole;
    }

    public SubRole getSubRole() {
        return subRole;
    }

    public void setSubRole(SubRole subRole) {
        this.subRole = subRole;
    }

    public String getEffectiveRoleName() {
        if (userRole != null) {
            return userRole.getRoleName();
        }
        return role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getEffectiveRoleName());
    }

    public String[] getAllowedProjects() {
        return allowedProjects != null ? allowedProjects : new String[]{};
    }

    public void setAllowedProjects(String[] allowedProjects) {
        this.allowedProjects = allowedProjects;
    }

    public String[] getAllowedSensitivity() {
        return allowedSensitivity != null ? allowedSensitivity : new String[] { "Public" };
    }

    public void setAllowedSensitivity(String[] allowedSensitivity) {
        this.allowedSensitivity = allowedSensitivity;
    }

    public List<String> getAllowedProjectsList() {
        String[] projects = getAllowedProjects();
        List<String> list = new ArrayList<>();

        for (String p : projects) {
            if (p != null && !p.trim().isEmpty()) {
                list.add(p);
            }
        }

        return list;
    }

    public List<String> getAllowedSensitivityList() {
        String[] sens = getAllowedSensitivity();
        List<String> list = new ArrayList<>();

        for (String s : sens) {
            if (s != null && !s.trim().isEmpty()) {
                list.add(s);
            }
        }

        return list;
    }
}
