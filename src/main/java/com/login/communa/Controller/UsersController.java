package com.login.communa.Controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.login.communa.Entity.Users;
import com.login.communa.Security.JwtUtil;
import com.login.communa.Service.EmailService;
import com.login.communa.Service.UserService;

@RestController
@CrossOrigin(origins = "*")
public class UsersController {

    private static final Logger logger = LoggerFactory.getLogger(UsersController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/addUser")
    public ResponseEntity<?> addUsers(@RequestBody Users user) {
        try {
            Users savedUser = userService.addUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("email", savedUser.getEmail());
            response.put("name", savedUser.getName());
            response.put("admissionNumber", savedUser.getAdmissionNumber());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error adding user: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/loginUser")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        if (email == null || email.isEmpty()) {
            email = loginRequest.get("userId");
        }
        
        String password = loginRequest.get("password");
        
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }
        
        Users user = userService.authenticateUser(email, password);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(user.getEmail(), "USER");
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("admissionNumber", user.getAdmissionNumber());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        logger.info("Forgot password request received for email: {}", email);
        
        if (email == null || email.isEmpty()) {
            logger.warn("Empty email in forgot password request");
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        
        try {
            // First check if user exists
            String token = userService.generateResetToken(email);
            logger.info("Reset token generated: {}", token);
            
            try {
                // Try to send email
                emailService.sendResetEmail(email, token);
                logger.info("Reset email sent successfully to: {}", email);
                return ResponseEntity.ok(Map.of("message", "Password reset link sent to your email"));
            } catch (Exception emailEx) {
                // Email sending failed - log the actual error
                logger.error("Failed to send email to {}: {}", email, emailEx.getMessage(), emailEx);
                return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to send email: " + emailEx.getMessage()
                ));
            }
        } catch (RuntimeException e) {
            // User not found
            logger.error("User not found: {}", email);
            return ResponseEntity.status(404).body(Map.of("error", "User not found with this email"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        
        logger.info("Reset password request received with token: {}", token);
        
        if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token and new password are required"));
        }
        
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters"));
        }
        
        boolean success = userService.resetPassword(token, newPassword);
        
        if (success) {
            logger.info("Password reset successful for token: {}", token);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } else {
            logger.warn("Invalid or expired token: {}", token);
            return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired token"));
        }
    }
}