package com.example.authdemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authdemo.model.UserFile;

public interface UserFileRepository extends JpaRepository<UserFile, Long> {
    
}
