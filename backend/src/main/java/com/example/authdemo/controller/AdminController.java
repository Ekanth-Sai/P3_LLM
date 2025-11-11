package com.example.authdemo.controller;

import com.example.authdemo.model.Role;
import com.example.authdemo.model.User;
import com.example.authdemo.model.UserFile;
import com.example.authdemo.repository.RoleRepository;
import com.example.authdemo.repository.UserFileRepository;
import com.example.authdemo.repository.UserRepository;
import com.example.authdemo.service.RBACService;
import com.example.authdemo.service.UserFileService;

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

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {
    private UserRepository userRepository;
    private UserFileRepository userFileRepository;
    private UserFileService userFileService;
    private RoleRepository roleRepository;
    private RBACService rbacService;

    public AdminController(UserRepository userRepository,
            UserFileRepository userFileRepository,
            UserFileService userFileService,
            RoleRepository roleRepository,
            RBACService rbacService) {
        this.userRepository = userRepository;
        this.userFileRepository = userFileRepository;
        this.userFileService = userFileService;
        this.roleRepository = roleRepository;
        this.rbacService = rbacService;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        List<User> users = userRepository.findByStatus("ACTIVE");
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("email", user.getEmail());
            map.put("firstName", user.getFirstName());
            map.put("lastName", user.getLastName());
            map.put("department", user.getDepartment());
            map.put("project", user.getProject());
            map.put("designation", user.getDesignation());
            map.put("role", user.getEffectiveRoleName());

            if (user.getUserRole() != null) {
                map.put("roleId", user.getUserRole().getId());
                map.put("roleDisplayName", user.getUserRole().getDisplayName());
            }

            if (user.getSubRole() != null) {
                map.put("subRoleId", user.getSubRole().getId());
                map.put("subRoleName", user.getSubRole().getDisplayName());
            }

            result.add(map);
        }
        return result;
    }

    @GetMapping("/users/{id}")
    public Optional<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent())
            return ResponseEntity.notFound().build();

        User user = optionalUser.get();

        if (updates.containsKey("email")) {
            String newEmail = (String) updates.get("email");
            Optional<User> existingUser = userRepository.findByEmail(newEmail);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                return ResponseEntity.status(409).body(Collections.singletonMap("error", "Email is already in use"));
            }
            user.setEmail(newEmail);
        }

        if (updates.containsKey("roleId")) {
            Long roleId = Long.valueOf(updates.get("roleId").toString());
            roleRepository.findById(roleId).ifPresent(user::setUserRole);
        }

        if (updates.containsKey("department")) {
            user.setDepartment((String) updates.get("department"));
        }

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
            map.put("firstName", user.getFirstName());
            map.put("lastName", user.getLastName());
            map.put("designation", user.getDesignation());
            map.put("department", user.getDepartment());
            map.put("project", user.getProject());
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

            // Assign default role if not set
            if (user.getUserRole() == null) {
                roleRepository.findByRoleName("DEVELOPER").ifPresent(user::setUserRole);
            }

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
    public ResponseEntity<?> uploadFile(
            @RequestParam String departmentName,
            @RequestParam String projectName,
            @RequestParam String sensitivity,
            @RequestParam(required = false) String allowedRolesJson,
            @RequestParam("file") MultipartFile file) {

        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "File size exceeds limit"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(
                "image/png", "image/jpeg", "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain").contains(contentType)) {
            return ResponseEntity.status(400).body(
                    Collections.singletonMap("error", "Invalid file type"));
        }

        try {
            String tempDir = "../temp_uploads/";
            String folderPathString = tempDir + departmentName + "/" + projectName;
            LocalDateTime dateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss");
            String currentDateTime = dateTime.format(formatter);
            String originalFileName = file.getOriginalFilename();
            String fileNameWithoutExt = originalFileName;
            String extension = "";
            Path folderPath = Paths.get(folderPathString);

            Files.createDirectories(folderPath);

            if (originalFileName != null) {
                int dotIndex = originalFileName.lastIndexOf('.');
                if (dotIndex != -1 && dotIndex < originalFileName.length() - 1) {
                    fileNameWithoutExt = originalFileName.substring(0, dotIndex);
                    extension = originalFileName.substring(dotIndex + 1);
                }
            }

            String finalName = fileNameWithoutExt + " d&t-" + currentDateTime + "." + extension;
            Path path = Paths.get(folderPath + "/" + finalName);

            Files.write(path, file.getBytes());
            userFileService.createUserFile(finalName, folderPath + "/" + finalName, projectName, departmentName);

            // Call Python API to process document
            RestTemplate restTemplate = new RestTemplate();
            String pythonApiUrl = "http://localhost:5001/process-document";
            Map<String, String> request = new HashMap<>();
            request.put("file_path", path.toString());
            request.put("department", departmentName);
            request.put("project", projectName);
            request.put("sensitivity", sensitivity);

            // Parse and expand roles if provided
            if (allowedRolesJson != null && !allowedRolesJson.isEmpty()) {
                // Frontend sends comma-separated role names
                List<String> selectedRoles = Arrays.asList(allowedRolesJson.split(","));
                List<String> expandedRoles = rbacService.expandRolesToIncludeParents(selectedRoles);
                request.put("allowed_roles", String.join(",", expandedRoles));
            }

            ResponseEntity<String> response = restTemplate.postForEntity(pythonApiUrl, request, String.class);

            return ResponseEntity.ok(Collections.singletonMap("status", "uploaded and processing started"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                    Collections.singletonMap("error", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/projects")
    public ResponseEntity<List<String>> getAllProjects() {
        List<String> projects = userFileService.getProjects();
        if (projects.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects/{department}")
    public ResponseEntity<List<String>> getProjectsByDepartment(@PathVariable String department) {
        List<String> projects = userFileService.getProjectByDepartment(department);
        if (projects.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/departments")
    public ResponseEntity<List<String>> getAllDepartments() {
        List<String> departments = userFileService.getDepartments();
        if (departments.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/files")
    public ResponseEntity<Map<String, Map<String, List<Map<String, Object>>>>> getFiles() {
        List<UserFile> files = userFileRepository.findAll();
        Map<String, Map<String, List<Map<String, Object>>>> grouped = new LinkedHashMap<>();

        for (UserFile file : files) {
            String department = (file.getDepartment() != null && !file.getDepartment().isBlank())
                    ? file.getDepartment()
                    : "Unassigned Department";
            String project = (file.getProject() != null && !file.getProject().isBlank())
                    ? file.getProject()
                    : "Unassigned Project";

            Map<String, Object> fileData = new HashMap<>();
            fileData.put("id", file.getId());
            fileData.put("filename", file.getFilename());
            fileData.put("path", file.getPath());

            grouped
                    .computeIfAbsent(department, d -> new LinkedHashMap<>())
                    .computeIfAbsent(project, p -> new ArrayList<>())
                    .add(fileData);
        }

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

    @DeleteMapping("/deleteFileByName/{fileName}")
    public ResponseEntity<?> deleteFileByName(@PathVariable String fileName) {
        try {
            Optional<UserFile> userFile = userFileService.findFileByFilename(fileName);
            Path path = Paths.get(userFile.get().getPath());
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                userFileRepository.delete(userFile.get());
                return ResponseEntity.ok(
                        Collections.singletonMap("message", "File deleted successfully: " + path));
            } else {
                return ResponseEntity.status(404).body("File not found: " + path);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error deleting file: " + e.getMessage());
        }
    }

    @GetMapping("/is-admin")
    public ResponseEntity<Map<String, Boolean>> isAdmin(@RequestParam String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        boolean isAdmin = optionalUser.isPresent() && optionalUser.get().isAdmin();

        Map<String, Boolean> response = new HashMap<>();
        response.put("is_admin", isAdmin);
        return ResponseEntity.ok(response);
    }
}