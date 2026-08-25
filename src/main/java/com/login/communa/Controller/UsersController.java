package com.login.communa.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.login.communa.Entity.Users;
import com.login.communa.Security.JwtUtil;
import com.login.communa.Service.EmailService;
import com.login.communa.Service.UserService;

import jakarta.validation.Valid;

@RestController
public class UsersController {

    private static final Logger logger = LoggerFactory.getLogger(UsersController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    /** Turn Bean Validation failures into a clean 400 JSON response. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    @PostMapping("/addUser")
    public ResponseEntity<?> addUsers(@Valid @RequestBody Users user) {
        try {
            Users savedUser = userService.addUser(user);

            // Send verification email (non-fatal if it fails)
            try {
                emailService.sendVerificationEmail(
                        Objects.requireNonNull(savedUser.getEmail(), "user email must not be null"),
                        Objects.requireNonNull(savedUser.getVerificationToken(), "verification token must not be null")
                );
                logger.info("Verification email sent to: {}", savedUser.getEmail());
            } catch (Exception emailEx) {
                logger.error("Failed to send verification email to {}: {}", savedUser.getEmail(), emailEx.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("email", savedUser.getEmail());
            response.put("name", savedUser.getName());
            response.put("admissionNumber", savedUser.getAdmissionNumber());
            response.put("universityRegisterNumber", savedUser.getUniversityRegisterNumber());
            response.put("collegeName", savedUser.getCollegeName());
            response.put("branch", savedUser.getBranch());
            response.put("department", savedUser.getDepartment());
            response.put("message", "Registration successful! Please check your email to verify your account.");

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

        Users user = null;
        try {
            user = userService.authenticateUser(email, password);
        } catch (RuntimeException e) {
            if ("EMAIL_NOT_VERIFIED".equals(e.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("error", "EMAIL_NOT_VERIFIED"));
            }
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(user.getEmail(), "USER");

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("admissionNumber", user.getAdmissionNumber());
        response.put("universityRegisterNumber", user.getUniversityRegisterNumber());
        response.put("collegeName", user.getCollegeName());
        response.put("branch", user.getBranch());
        response.put("department", user.getDepartment());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        String email = org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

        try {
            Users user = userService.getUserByEmail(email);

            Map<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("name", user.getName());
            response.put("admissionNumber", user.getAdmissionNumber());
            response.put("universityRegisterNumber", user.getUniversityRegisterNumber());
            response.put("collegeName", user.getCollegeName());
            response.put("branch", user.getBranch());
            response.put("department", user.getDepartment());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error loading profile: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> request) {
        String email = org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

        String admissionNumber = request.get("admissionNumber");
        String universityRegisterNumber = request.get("universityRegisterNumber");

        if (admissionNumber == null || admissionNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Admission number is required"));
        }

        if (universityRegisterNumber == null || universityRegisterNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "University register number is required"));
        }

        universityRegisterNumber = universityRegisterNumber.trim().toUpperCase();
        if (!universityRegisterNumber.matches("^[A-Z]{3}\\d{2}[A-Z]{2}\\d{3}$")) {
            return ResponseEntity.badRequest().body(Map.of(
                "error",
                "University register number must be in the form SCT24CS002"
            ));
        }

        try {
            Users updatedUser = userService.updateProfile(
                email,
                admissionNumber.trim(),
                universityRegisterNumber
            );

            Map<String, Object> response = new HashMap<>();
            response.put("email", updatedUser.getEmail());
            response.put("name", updatedUser.getName());
            response.put("admissionNumber", updatedUser.getAdmissionNumber());
            response.put("universityRegisterNumber", updatedUser.getUniversityRegisterNumber());
            response.put("collegeName", updatedUser.getCollegeName());
            response.put("branch", updatedUser.getBranch());
            response.put("department", updatedUser.getDepartment());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating profile: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        // Always return the same message to prevent user enumeration
        final String genericMessage = "If that email is registered, a reset link has been sent. Please check your inbox.";

        try {
            String token = userService.generateResetToken(email);

            try {
                emailService.sendResetEmail(
                        Objects.requireNonNull(email, "email must not be null"),
                        Objects.requireNonNull(token, "reset token must not be null")
                );
                logger.info("Password reset email sent to: {}", email);
            } catch (Exception emailEx) {
                logger.error("Failed to send reset email to {}: {}", email, emailEx.getMessage(), emailEx);
                return ResponseEntity.status(500).body(Map.of("error", "Failed to send email: " + emailEx.getMessage()));
            }

        } catch (RuntimeException e) {
            if ("RESET_TOO_SOON".equals(e.getMessage())) {
                return ResponseEntity.status(429).body(Map.of(
                    "error", "Please wait 60 seconds before requesting another reset link."
                ));
            }
            // USER_NOT_FOUND — fall through and return generic 200 to prevent enumeration
            logger.info("Forgot-password requested for unknown email (suppressed for security)");
        }

        return ResponseEntity.ok(Map.of("message", genericMessage));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token and new password are required"));
        }

        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters"));
        }

        boolean success = userService.resetPassword(token, newPassword);

        if (success) {
            logger.info("Password reset successful");
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } else {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired token"));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@org.springframework.web.bind.annotation.RequestParam("token") String token) {
        logger.info("Email verification request received");

        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Verification token is required"));
        }

        boolean success = userService.verifyEmail(token);

        if (success) {
            logger.info("Email verified successfully");
            return ResponseEntity.ok(Map.of("message", "Email verified successfully! You can now log in."));
        } else {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired verification token"));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        try {
            String token = userService.resendVerificationToken(email);

            try {
                emailService.sendVerificationEmail(
                        Objects.requireNonNull(email, "email must not be null"),
                        Objects.requireNonNull(token, "verification token must not be null")
                );
                logger.info("Verification email resent to: {}", email);
                return ResponseEntity.ok(Map.of("message", "Verification email sent! Please check your inbox."));
            } catch (Exception emailEx) {
                logger.error("Failed to resend verification email to {}: {}", email, emailEx.getMessage());
                return ResponseEntity.status(500).body(Map.of("error", "Failed to send email: " + emailEx.getMessage()));
            }
        } catch (RuntimeException e) {
            logger.error("Resend verification error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
