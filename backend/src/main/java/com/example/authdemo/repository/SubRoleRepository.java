package com.example.authdemo.repository;

import com.example.authdemo.model.SubRole;
import com.example.authdemo.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface SubRoleRepository extends JpaRepository<SubRole, Long> {
    Optional<SubRole> findBySubRoleName(String subRoleName);

    List<SubRole> findByParentRole(Role parentRole);

    List<SubRole> findByParentRoleId(Long parentRoleId);
    
}
