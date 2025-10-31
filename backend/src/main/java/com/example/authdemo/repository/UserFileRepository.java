package com.example.authdemo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authdemo.model.User;
import com.example.authdemo.model.UserFile;

public interface UserFileRepository extends JpaRepository<UserFile, Long> {
	Optional<UserFile> findByPath(String path);
	Optional<UserFile> findByFilename(String filename);
}
