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

    // ✅ Existing authenticate method modified to accept raw password
        public Admin authenticate(String email, String rawPassword) {
    Optional<Admin> adminOpt = repository.findByEmail(email);
    if (adminOpt.isEmpty()) {
        return null;
    }

    Admin admin = adminOpt.get();
    // Directly compare the raw password string
    if (rawPassword.equals(admin.getPassword())) { // This line now checks plain text
        return admin;
    }
    return null;
}
    // ✅ Safe wrapper method to support older "login()" calls
    public Admin login(String email, String rawPassword) {
        return authenticate(email, rawPassword);
    }

    public Admin createAdmin(Admin admin) {
    return repository.save(admin);
    }
}
