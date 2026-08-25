package com.login.communa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Communa Spring Boot application.
 *
 * @EnableAsync activates Spring's asynchronous method execution support,
 * required by EmailService#sendVerificationEmail and sendResetEmail.
 * Without this annotation, @Async methods execute synchronously, blocking
 * the HTTP request thread during SMTP handshakes.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.login.communa")
@EnableAsync
public class CommunaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommunaApplication.class, args);
	}

}
