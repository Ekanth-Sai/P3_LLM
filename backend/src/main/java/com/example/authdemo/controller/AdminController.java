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
    private UserFileService userFileService;
    
    public AdminController(UserRepository userRepository, UserFileRepository userFileRepository,UserFileService userFileService) {
		super();
		this.userRepository = userRepository;
		this.userFileRepository = userFileRepository;
		this.userFileService = userFileService;
	}




    @GetMapping("/users")
    public List<User> getUsers() {

        List<User> users = userRepository.findByStatus("ACTIVE");
//        List<Map<String, Object>> result = new ArrayList<>();
//        for (User user : users) {
//            Map<String, Object> map = new HashMap<>();
//            map.put("id", user.getId());
//            map.put("email", user.getEmail());
//            map.put("role", user.getRole());
//            result.add(map);
//        }
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
    public ResponseEntity<?> uploadFile(@RequestParam String projectName,@RequestParam("file") MultipartFile file) {

        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "File size exceeds limit"));
        }
        
        String contentType = file.getContentType();
        if(contentType == null || !Arrays.asList("image/png", "image/jpeg", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain").contains(contentType)) {
            return ResponseEntity.status(400).body(Collections.singletonMap("error", "Invalid file type. Only PNG, JPEG, PDF, DOCX, and XLSX are allowed."));
        }
        
        try {
            String tempDir = "/Users/harshakuppala/Desktop/P3_LLM/temp_uploads/";
            String folderPathString = tempDir+projectName;
            LocalDateTime dateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss");
            String currentDateTime = dateTime.format(formatter);
            String originalFileName = file.getOriginalFilename();
            String fileNameWithoutExt = originalFileName;
            String extension = "";
            Path folderPath = Paths.get(folderPathString);
          try {
              //will create all non-existent parent directories too
              Files.createDirectories(folderPath);
             // System.out.println("✅ Folder created at: " + folderPath.toAbsolutePath());
          } catch (IOException e) {
              System.err.println("❌ Failed to create folder: " + e.getMessage());
          }


            if (originalFileName != null) {
                int dotIndex = originalFileName.lastIndexOf('.');
                if (dotIndex != -1 && dotIndex < originalFileName.length() - 1) {
                    fileNameWithoutExt = originalFileName.substring(0, dotIndex);
                    extension = originalFileName.substring(dotIndex + 1);
                }
            }
            String finalName =  fileNameWithoutExt+" d&t-"+currentDateTime+"."+extension;
            Path path = Paths.get(folderPath+"/"+finalName );
            try {
                Files.write(path, file.getBytes());
                userFileService.createUserFile(finalName,folderPath+"/"+finalName,projectName);
               // System.err.println(" created file: " + path.toAbsolutePath());
            } catch (IOException e) {
               // System.err.println("❌ Failed to create file: " + e.getMessage());
            }

            RestTemplate restTemplate = new RestTemplate();
            String pythonApiUrl = "http://localhost:5001/process-document";
            HashMap<String, String> request = new HashMap<>();
            request.put("file_path", path.toString());
            ResponseEntity<String> response = restTemplate.postForEntity(pythonApiUrl, request, String.class);
            return ResponseEntity.ok(Collections.singletonMap("status", "uploaded and processing started"));
        } catch (Exception e) {
        	e.printStackTrace(); 
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Upload failed: " + e.getMessage()));
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
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(resource);
    }

//    List<UserFile> files = userFileRepository.findAll();
//    Map<String, List<UserFile>> grouped = files.stream()
//        .collect(Collectors.groupingBy(UserFile::getProject));
//    return ResponseEntity.ok(grouped);
//    @GetMapping("/admin/files")
//    public ResponseEntity<?> getAllFiles() {
//        try {
//            List<UserFile> userFilesList = userFileService.getAllFiles();
//            System.out.println("files called");
//            if (userFilesList.isEmpty()) {
//                return ResponseEntity.status(204).body("⚠️ No files found");
//            }
//
//            return ResponseEntity.ok(userFilesList);
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError()
//                    .body(" Failed to fetch files: " + e.getMessage());
//        }
//    }
    
    @DeleteMapping("/deleteFileByName/{fileName}")
    public ResponseEntity<?> deleteFileByName(@PathVariable String fileName) {
        try {
        	Optional<UserFile> userFile=userFileService.findFileByFilename(fileName);
        	Path path = Paths.get(userFile.get().getPath());
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
            	userFileRepository.delete(userFile.get());
                //System.out.println("✅ File deleted successfully: " + path);
                return ResponseEntity.ok(Collections.singletonMap("message", "✅ File deleted successfully: " + path));
            } else {
               // System.out.println("⚠️ File not found: " + path);
                return ResponseEntity.status(404).body("⚠️ File not found: " + path);
            }
        } catch (Exception e) {
            //System.err.println("❌ Error deleting file: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("❌ Error deleting file: " + e.getMessage());
        }
    }
    
    @GetMapping("/is-admin")
    public ResponseEntity<Map<String, Boolean>> isAdmin(@RequestParam String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        boolean isAdmin = optionalUser.isPresent() &&
                          optionalUser.get().getRole().equalsIgnoreCase("admin");
        Map<String, Boolean> response = new HashMap<>();
        response.put("is_admin", isAdmin);
        return ResponseEntity.ok(response);
    }

    
    
  

}
