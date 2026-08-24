package com.login.communa.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.login.communa.Entity.Users;
import com.login.communa.Repository.UsersRepo;

@Service
public class UserService {

    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** How many seconds a user must wait between forgot-password requests. */
    private static final long RESET_COOLDOWN_SECONDS = 60;

    public Users addUser(Users user) {
        Optional<Users> existing = usersRepo.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set up email verification
        user.setEmailVerified(false);
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        return usersRepo.save(user);
    }

    public Users authenticateUser(String email, String rawPassword) {
        Optional<Users> userOpt = usersRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            return null;
        }

        Users user = userOpt.get();

        // Check if email is verified
        if (!user.isEmailVerified()) {
            throw new RuntimeException("EMAIL_NOT_VERIFIED");
        }

        // Check if password matches
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return user;
        }
        return null;
    }

    public Users getUserByEmail(String email) {
        return usersRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Department code → full name mapping.
     */
    private static final java.util.Map<String, String> DEPT_MAP = java.util.Map.ofEntries(
        java.util.Map.entry("CS", "Computer Science"),
        java.util.Map.entry("IT", "Information Technology"),
        java.util.Map.entry("EC", "Electronics & Communication"),
        java.util.Map.entry("EE", "Electrical Engineering"),
        java.util.Map.entry("ME", "Mechanical Engineering"),
        java.util.Map.entry("CE", "Civil Engineering"),
        java.util.Map.entry("CH", "Chemical Engineering"),
        java.util.Map.entry("BT", "Biotechnology"),
        java.util.Map.entry("AI", "Artificial Intelligence"),
        java.util.Map.entry("AD", "AI & Data Science"),
        java.util.Map.entry("CY", "Cyber Security"),
        java.util.Map.entry("RA", "Robotics & Automation")
    );

    /**
     * College prefix → full name mapping.
     */
    private static final java.util.Map<String, String> COLLEGE_MAP = java.util.Map.of(
        "SCT", "Saintgits College of Engineering"
    );

    public Users updateProfile(String email, String admissionNumber, String universityRegisterNumber) {
        Optional<Users> userOpt = usersRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        Users user = userOpt.get();
        user.setAdmissionNumber(admissionNumber);
        user.setUniversityRegisterNumber(universityRegisterNumber);

        // Auto-extract college, department, and branch from the register number
        // Format: SCT24CS002 → college(SCT) year(24) dept(CS) roll(002)
        if (universityRegisterNumber != null && universityRegisterNumber.matches("^[A-Z]{3}\\d{2}[A-Z]{2}\\d{3}$")) {
            String collegeCode = universityRegisterNumber.substring(0, 3);
            String deptCode = universityRegisterNumber.substring(5, 7);

            user.setCollegeName(COLLEGE_MAP.getOrDefault(collegeCode, collegeCode));
            String deptFull = DEPT_MAP.getOrDefault(deptCode, deptCode);
            user.setDepartment(deptFull);
            user.setBranch(deptFull); // branch = department for most colleges
        }

        return usersRepo.save(user);
    }

    /**
     * Generate a password-reset token for the given email.
     * Enforces a {@value #RESET_COOLDOWN_SECONDS}-second cooldown per user to prevent abuse.
     *
     * @throws RuntimeException with code "USER_NOT_FOUND" if no account matches.
     * @throws RuntimeException with code "RESET_TOO_SOON" if within the cooldown window.
     */
    public String generateResetToken(String email) {
        Optional<Users> userOpt = usersRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("USER_NOT_FOUND");
        }

        Users user = userOpt.get();

        // Enforce cooldown to prevent inbox-flooding and token invalidation attacks
        if (user.getPasswordResetRequestedAt() != null) {
            long secondsSinceLast = java.time.Duration
                .between(user.getPasswordResetRequestedAt(), LocalDateTime.now())
                .toSeconds();
            if (secondsSinceLast < RESET_COOLDOWN_SECONDS) {
                throw new RuntimeException("RESET_TOO_SOON");
            }
        }

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(1));
        user.setPasswordResetRequestedAt(LocalDateTime.now());
        usersRepo.save(user);

        return token;
    }

    public boolean resetPassword(String token, String newPassword) {
        Optional<Users> userOpt = usersRepo.findByResetToken(token);
        if (userOpt.isEmpty()) {
            return false;
        }

        Users user = userOpt.get();

        // Check if token is expired
        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Hash and update password, then clear the reset token
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setTokenExpiry(null);
        usersRepo.save(user);

        return true;
    }

    /**
     * Verify a user's email using the verification token.
     */
    public boolean verifyEmail(String token) {
        Optional<Users> userOpt = usersRepo.findByVerificationToken(token);
        if (userOpt.isEmpty()) {
            return false;
        }

        Users user = userOpt.get();

        // Check if token is expired
        if (user.getVerificationTokenExpiry() != null &&
            user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Mark email as verified and clear the token
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        usersRepo.save(user);

        return true;
    }

    /**
     * Resend verification email by generating a new token.
     */
    public String resendVerificationToken(String email) {
        Optional<Users> userOpt = usersRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        Users user = userOpt.get();

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        usersRepo.save(user);

        return token;
    }
}
