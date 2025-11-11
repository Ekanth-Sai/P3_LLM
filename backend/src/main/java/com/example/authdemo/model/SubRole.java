package com.example.authdemo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sub_roles")
public class SubRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_role_name", unique = true, nullable = false)
    private String subRoleName;

    @ManyToOne
    @JoinColumn(name = "parent_role_id", nullable = false)
    private Role parentRole;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "allowed_sensitivity", columnDefinition = "text[]")
    private String[] allowedSensitivity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubRoleName() {
        return subRoleName;
    }

    public void setSubRoleName(String subRoleName) {
        this.subRoleName = subRoleName;
    }

    public Role getParentRole() {
        return parentRole;
    }

    public void setParentRole(Role parentRole) {
        this.parentRole = parentRole;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getAllowedSensitivity() {
        return allowedSensitivity != null ? allowedSensitivity : new String[] {};
    }

    public void setAllowedSensitivity(String[] allowedSensitivity) {
        this.allowedSensitivity = allowedSensitivity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getAllowedSensitivityList() {
        List<String> list = new ArrayList<>();

        if (allowedSensitivity != null) {
            for (String s : allowedSensitivity) {
                if (s != null && !s.trim().isEmpty()) {
                    list.add(s);
                }
            }
        }

        return list;
    }
}
