package com.example.authdemo.controller;

import com.example.authdemo.model.User;
import com.example.authdemo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    // Existing users
    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        List<User> users = userRepository.findByPending(false);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("email", user.getEmail());
            map.put("role", user.getRole());
            result.add(map);
        }
        return result;
    }

    // Update user
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent())
            return ResponseEntity.notFound().build();

        User user = optionalUser.get();
        if (updates.containsKey("email"))
            user.setEmail(updates.get("email"));
        if (updates.containsKey("role"))
            user.setRole(updates.get("role"));
        userRepository.save(user);
        return ResponseEntity.ok(Collections.singletonMap("status", "updated"));
    }

    // Pending users
    @GetMapping("/pending-users")
    public List<Map<String, Object>> getPendingUsers() {
        List<User> users = userRepository.findByPending(true);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("email", user.getEmail());
            result.add(map);
        }
        return result;
    }

    // Accept or decline pending user
    @PostMapping("/pending-users/{id}")
    public ResponseEntity<?> handlePendingUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent())
            return ResponseEntity.notFound().build();

        User user = optionalUser.get();
        String action = body.get("action"); // "accept" or "decline"

        if ("accept".equalsIgnoreCase(action)) {
            user.setPending(false);
            userRepository.save(user);
            return ResponseEntity.ok(Collections.singletonMap("status", "accepted"));
        } else if ("decline".equalsIgnoreCase(action)) {
            String reason = body.get("reason");

            // send decline email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Your account request was declined");
            message.setText("Your account request was declined for the following reason: " + reason);
            mailSender.send(message);

            userRepository.delete(user);
            return ResponseEntity.ok(Collections.singletonMap("status", "declined"));
        } else {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid action"));
        }
    }
}
