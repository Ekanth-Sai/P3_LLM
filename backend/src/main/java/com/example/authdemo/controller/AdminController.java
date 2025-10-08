package com.example.authdemo.controller;

import com.example.authdemo.model.User;
import com.example.authdemo.model.UserFile;
import com.example.authdemo.repository.UserFileRepository;
import com.example.authdemo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFileRepository userFileRepository;

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        List<User> users = userRepository.findByStatus("ACTIVE");
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

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent())
            return ResponseEntity.notFound().build();

        User user = optionalUser.get();
        if (updates.containsKey("email")) {
            String newEmail = updates.get("email");
            Optional<User> existingUser = userRepository.findByEmail(newEmail);

            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                return ResponseEntity.status(409).body(Collections.singletonMap("error", "Email is already in use"));
            }
            user.setEmail(newEmail);
        }
        if (updates.containsKey("role"))
            user.setRole(updates.get("role"));
        userRepository.save(user);
        return ResponseEntity.ok(Collections.singletonMap("status", "updated"));
    }

    @GetMapping("/pending-users")
    public List<Map<String, Object>> getPendingUsers() {
        List<User> users = userRepository.findByStatus("PENDING");
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("email", user.getEmail());
            result.add(map);
        }
        return result;
    }

    @PostMapping("/pending-users/{id}")
    public ResponseEntity<?> handlePendingUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent())
            return ResponseEntity.notFound().build();

        User user = optionalUser.get();
        String action = body.get("action"); // "accept" or "decline"

        if ("accept".equalsIgnoreCase(action)) {
            user.setStatus("ACTIVE");
            userRepository.save(user);
            return ResponseEntity.ok(Collections.singletonMap("status", "accepted"));
        } else if ("decline".equalsIgnoreCase(action)) {
            user.setStatus("DECLINED");
            userRepository.save(user);
            return ResponseEntity.ok(Collections.singletonMap("status", "declined"));
        } else {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid action"));
        }
    }

    @PostMapping("/upload-file")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {

        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "File size exceeds limit"));
        }

        String contentType = file.getContentType();


        if(contentType == null || !Arrays.asList("image/png", "image/jpeg", "application/pdf").contains(contentType)) {
            return ResponseEntity.status(400).body(Collections.singletonMap("error", "Invalid file type. Only PNG, JPEG, and PDF are allowed."));
        }

        try {
            UserFile userFile = new UserFile();
            userFile.setFilename(file.getOriginalFilename());
            userFile.setData(file.getBytes());
            userFileRepository.save(userFile);
            return ResponseEntity.ok(Collections.singletonMap("status", "uploaded"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Upload failed"));
        }
    }

    @GetMapping("/download-file/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        Optional<UserFile> userFileOpt = userFileRepository.findById(id);
        if (!userFileOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        UserFile userFile = userFileOpt.get();

        String filename = userFile.getFilename().replaceAll("[^a-zA-Z0-9.-]", "_");
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
            .body(userFile.getData());
    }
}
