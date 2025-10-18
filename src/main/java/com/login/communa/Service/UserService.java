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
    
    public Users addUser(Users user) {
        Optional<Users> existing = usersRepo.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        
        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return usersRepo.save(user);
    }

    public Users authenticateUser(String email, String rawPassword) {
        Optional<Users> userOpt = usersRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            return null;
        }
        
        Users user = userOpt.get();
        // Check if password matches
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return user;
        }
        return null;
    }
    
    public String generateResetToken(String email) {
        Optional<Users> userOpt = usersRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        Users user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(1)); // Token valid for 1 hour
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
        
        // Hash and update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setTokenExpiry(null);
        usersRepo.save(user);
        
        return true;
    }
}