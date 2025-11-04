package com.example.authdemo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.authdemo.model.UserFile;

public interface UserFileRepository extends JpaRepository<UserFile, Long> {
	Optional<UserFile> findByPath(String path);
	Optional<UserFile> findByFilename(String filename);
	@Query("SELECT DISTINCT uf.project FROM UserFile uf WHERE uf.project IS NOT NULL")
	List<String> findDistinctProjects();
	@Query("SELECT DISTINCT uf.department FROM UserFile uf WHERE uf.department IS NOT NULL")
	List<String> findDistinctDepartments();
	@Query("SELECT DISTINCT u.project FROM UserFile u WHERE u.department = :department AND u.project IS NOT NULL")
	List<String> findDistinctProjectsByDepartment(String department);

}
