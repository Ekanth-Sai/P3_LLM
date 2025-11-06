package com.example.authdemo.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authdemo.model.UserFile;

public interface UserFileRepository extends JpaRepository<UserFile, Long> {
	Optional<UserFile> findByPath(String path);
	Optional<UserFile> findByFilename(String filename);
	@Query("SELECT DISTINCT uf.department FROM UserFile uf WHERE uf.department IS NOT NULL ORDER BY uf.department")
	List<String> findDistinctDepartments();
	@Query("SELECT DISTINCT uf.project FROM UserFile uf WHERE uf.project IS NOT NULL ORDER BY uf.project")
	List<String> findDistinctProjects();
	@Query("SELECT DISTINCT uf.project FROM UserFile uf WHERE uf.department = :department AND uf.project IS NOT NULL ORDER BY uf.project")
	List<String> findDistinctProjectsByDepartment(@Param("department") String department);

}
