package com.login.communa.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.login.communa.Entity.Admin;
import com.login.communa.Service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");
        
        // Validation
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        Admin admin = service.authenticate(email, password);

        if (admin != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("clubName", admin.getClubName());
            response.put("email", admin.getEmail());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }
    
    // ✅ NEW: Endpoint to create admin accounts (SECURE THIS IN PRODUCTION!)
    @PostMapping("/create")
    public ResponseEntity<?> createAdmin(@RequestBody Admin admin) {
        try {
            Admin created = service.createAdmin(admin);
            return ResponseEntity.ok(Map.of(
                "message", "Admin created successfully",
                "email", created.getEmail(),
                "clubName", created.getClubName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}