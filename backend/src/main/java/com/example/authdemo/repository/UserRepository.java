package com.example.authdemo.repository;

import com.example.authdemo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndPassword(String email, String password);

    List<User> findByPending(boolean pending);
    
    Optional<User> findByEmail(String email);
}
