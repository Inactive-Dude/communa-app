package com.login.communa.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.login.communa.Entity.Admin;
import com.login.communa.Repository.AdminRepository;

@Service
public class AdminService {

    @Autowired
    private AdminRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Authenticate an admin with their email and raw (plain-text) password.
     * Passwords are stored as BCrypt hashes; plain-text comparison is NOT used.
     */
    public Admin authenticate(String email, String rawPassword) {
        Optional<Admin> adminOpt = repository.findByEmail(email);
        if (adminOpt.isEmpty()) {
            return null;
        }

        Admin admin = adminOpt.get();
        if (passwordEncoder.matches(rawPassword, admin.getPassword())) {
            return admin;
        }
        return null;
    }

    /**
     * Create a new admin account. The password is BCrypt-hashed before saving.
     * Throws if the email is already registered.
     */
    public Admin createAdmin(Admin admin) {
        if (repository.findByEmail(admin.getEmail()).isPresent()) {
            throw new RuntimeException("An admin with this email already exists.");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return repository.save(admin);
    }
}
