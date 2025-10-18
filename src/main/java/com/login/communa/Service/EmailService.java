package com.login.communa.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Add this to get the sender email from properties
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendResetEmail(String toEmail, String token) {
        String resetUrl = frontendUrl + "/reset-password.html?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail); // ✅ THIS IS THE FIX - Set the from address
        message.setTo(toEmail);
        message.setSubject("Password Reset Request - Communa");
        message.setText("Hello,\n\n" +
                "You requested to reset your password. Click the link below to reset it:\n\n" +
                resetUrl + "\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Best regards,\nCommuna Team");

        mailSender.send(message);
    }
}