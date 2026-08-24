package com.login.communa.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.login.communa.Entity.Admin;
import com.login.communa.Security.JwtUtil;
import com.login.communa.Service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService service;
    private final JwtUtil jwtUtil;

    @Value("${admin.create.secret}")
    private String adminCreateSecret;

    public AdminController(AdminService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticate an admin and return a JWT with ROLE_ADMIN.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        Admin admin = service.authenticate(email, password);

        if (admin != null) {
            // Issue a JWT so admin pages can call protected endpoints
            String token = jwtUtil.generateToken(admin.getEmail(), "ADMIN");

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("token", token);
            response.put("clubName", admin.getClubName());
            response.put("email", admin.getEmail());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    /**
     * Create a new admin account.
     * Protected by a shared secret header — set ADMIN_CREATE_SECRET env var before use.
     * This endpoint should be used only during initial setup and then the secret rotated.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createAdmin(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
            @RequestBody Admin admin) {

        if (secret == null || !secret.equals(adminCreateSecret)) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: invalid or missing admin secret"));
        }

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