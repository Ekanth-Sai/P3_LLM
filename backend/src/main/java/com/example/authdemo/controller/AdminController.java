package com.example.authdemo.controller;

import com.example.authdemo.model.User;
import com.example.authdemo.model.UserFile;
import com.example.authdemo.repository.UserFileRepository;
import com.example.authdemo.repository.UserRepository;
import com.example.authdemo.service.UserFileService;

import org.slf4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {


    private UserRepository userRepository;
    private UserFileRepository userFileRepository;
    
    public AdminController(UserRepository userRepository, UserFileRepository userFileRepository) {
		super();
		this.userRepository = userRepository;
		this.userFileRepository = userFileRepository;
	}


    // @GetMapping("/files")
    // public ResponseEntity<?> getFiles() {
    //     try {
    //         RestTemplate restTemplate = new RestTemplate();
    //         String pythonApiUrl = "http://localhost:5001/processed-documents";

    //         ResponseEntity<String[]> response = restTemplate.getForEntity(pythonApiUrl, String[].class);

    //         List<Map<String, String>> filesList = new ArrayList<>();

    //         if(response.getBody() != null) {
    //             for(String filepath : response.getBody()) {
    //                 Map<String, String> fileMap = new HashMap<>();

    //                 String filename = filepath.substring(filepath.lastIndexOf('/') + 1);
    //                 fileMap.put("filename", filename);
    //                 fileMap.put("path", filepath);
    //                 filesList.add(fileMap);
    //             }
    //         }

    //         return ResponseEntity.ok(filesList);
    //     } catch (Exception e) {
    //         return ResponseEntity.status(500)
    //                 .body(Collections.singletonMap("error", "Failed to fetch files: " + e.getMessage()));
    //     }
    // }
    // public List<Map<String, Object>> getFiles() {
    //     List<UserFile> files = userFileRepository.findAll();
    //     List<Map<String, Object>> result = new ArrayList<>();
    //     for (UserFile file : files) {
    //         Map<String, Object> map = new HashMap<>();
    //         map.put("id", file.getId());
    //         map.put("filename", file.getFilename());
    //         result.add(map);
    //     }
    //     return result;
    // }

    @DeleteMapping("/files/{filename}")
    public ResponseEntity<?> deleteFile(@PathVariable String filename) {
        try {
            RestTemplate  restTemplate = new RestTemplate();
            String pythonApiUrl = "http://localhost:5001/delete-document";

            Map<String, String> request = new HashMap<>();
            request.put("filename", filename);

            ResponseEntity<String> response = restTemplate.postForEntity(pythonApiUrl, request, String.class);

            return ResponseEntity.ok(Collections.singletonMap("status", "deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to deete: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        List<User> users = userRepository.findByStatus("ACTIVE");
        return users;
    }

    @GetMapping("/users/{id}")
    public Optional<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id);
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
        String action = body.get("action");

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
    	System.out.println("📥 uploadFile() method entered");

        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "File size exceeds limit"));
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList("image/png", "image/jpeg", "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain").contains(contentType)) {
            return ResponseEntity.status(400).body(Collections.singletonMap("error",
                    "Invalid file type. Only PNG, JPEG, PDF, DOCX, XLSX, and TXT are allowed."));
        }

        try {
            String tempDir = "/home/ekanthsai/Desktop/P3_LLM/temp_uploads/";
            Path path = Paths.get(tempDir + file.getOriginalFilename());
            Files.write(path, file.getBytes());
            RestTemplate restTemplate = new RestTemplate();
            String pythonApiUrl = "http://localhost:5001/process-document";
            HashMap<String, String> request = new HashMap<>();
            request.put("file_path", path.toString());

            ResponseEntity<String> response = restTemplate.postForEntity(pythonApiUrl, request, String.class);
            return ResponseEntity.ok(Collections.singletonMap("status", "uploaded and processing started"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Upload failed: " + e.getMessage()));
        }
    }
    
//
//    @GetMapping("/download-file/{id}")
//    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
//        Optional<UserFile> userFileOpt = userFileRepository.findById(id);
//        if (!userFileOpt.isPresent()) {
//            return ResponseEntity.notFound().build();
//        }
//        UserFile userFile = userFileOpt.get();
//        String filename = userFile.getFilename().replaceAll("[^a-zA-Z0-9.-]", "_");
//        return ResponseEntity.ok()
//            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
//            .body(userFile.getData());
//    }
    
    @GetMapping("/files")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getFiles() {
        List<UserFile> files = userFileRepository.findAll();

        Map<String, List<Map<String, Object>>> grouped = files.stream()
            .collect(Collectors.groupingBy(
                file -> {
                    String project = file.getProject();
                    return (project != null && !project.isBlank()) ? project : "Unassigned";
                },
                LinkedHashMap::new,  // preserve insertion order (optional)
                Collectors.mapping(file -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", file.getId());
                    map.put("filename", file.getFilename());
                    map.put("path", file.getPath());
                    return map;
                }, Collectors.toList())
            ));

        return ResponseEntity.ok(grouped);
    }
    
    @GetMapping("/download-file/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws IOException {
    	Optional<UserFile> userFile = userFileRepository.findByFilename(filename);
        Path path = Paths.get(userFile.get().getPath());
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(userFile.getData());
    }
    
    // @DeleteMapping("/files/{filename}")
    // public ResponseEntity<?> deleteFile(@PathVariable String filename) {
    //     try {
    //         RestTemplate restTemplate = new RestTemplate();
    //         String pythonApiUrl = "http://localhost:5001/delete-document";

    //         Map<String, String> request = new HashMap<>();
    //         request.put("filename", filename);

    //         ResponseEntity<String> response = restTemplate.postForEntity(pythonApiUrl, request, String.class);

    //         return ResponseEntity.ok(Collections.singletonMap("status", "deleted"));
    //     } catch (Exception e) {
    //         return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to delete: " + e.getMessage()));
    //     }
    // }
}

