package com.example.authdemo.repository;

import com.example.authdemo.model.Role;
import com.example.authdemo.model.SubRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String roleName);

    List<Role> findByParentRoleIsNull();

    List<Role> findByParentRole(Role parentRole);

    @Query(value = "SELECT * FROM get_parent_roles(:roleId)", nativeQuery = true)
    List<Object[]> getParentRolesNative(@Param("roleId") Long roleId);

    @Query(value = "SELECT * FROM get_child_roles(:roleId)", nativeQuery = true)
    List<Object[]> getChildRolesNative(@Param("roleId") Long roleId);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.parentRole LEFT JOIN FETCH r.childRoles")
    List<Role> findAllWithHierarchy();
    
}
