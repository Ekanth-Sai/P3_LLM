package com.example.authdemo.controller;

import com.example.authdemo.service.RBACService;
import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class QueryController {

    private final RBACService rbacService;

    public QueryController(RBACService rbacService) {
        this.rbacService = rbacService;
    }
    
    @PostMapping("/query")
    public ResponseEntity<?> query(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String query = payload.get("query");

        if (username == null || query == null) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "username and query are required"));
        }
        
        try {
            Map<String, Object> filters = rbacService.getUserFilters(username);

            System.out.println("RBAC filters for " + username + ": " + filters);
            
            Map<String, Object> enrichedPayload = new HashMap<>();
            enrichedPayload.put("Username", username);
            enrichedPayload.put("query", query);
            enrichedPayload.put("filters", filters);

            RestTemplate restTemplate = new RestTemplate();
            String pythonApiUrl = "http://localhost:5001/query";

            ResponseEntity<String> response = restTemplate.postForEntity(pythonApiUrl, payload, String.class);

            return response; 
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(500).body(
                    Collections.singletonMap("error", "failed to get response from query service: " + e.getMessage()));
        }
    }
    
}
