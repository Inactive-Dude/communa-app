package com.login.communa.Security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    /** Comma-separated origins. Set app.cors.allowed-origins in application.yml or via env var. */
    @Value("${app.cors.allowed-origins:http://localhost:8082}")
    private List<String> corsAllowedOrigins;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public API endpoints (no JWT required)
                .requestMatchers(
                    "/addUser",
                    "/loginUser",
                    "/forgot-password",
                    "/reset-password",
                    "/verify-email",
                    "/resend-verification",
                    "/api/admin/login"
                ).permitAll()

                // Public pages — accessible without login
                .requestMatchers(
                    "/", "/index.html", "/newpage.html", "/Profile.html",
                    "/Help.html", "/AboutUs.html", "/Clubs.html",
                    "/forgot-password.html", "/reset-password.html", "/verify-email.html",
                    "/favicon.ico"
                ).permitAll()

                // Club public pages (HTML) — any authenticated user
                .requestMatchers(
                    "/**-announcements.html", "/**-events.html",
                    "/Coding Club.html", "/IEEE.html", "/NSS.html",
                    "/CSI.html", "/IEDC.html", "/Meckartans.html", "/Tinker Hub.html",
                    "/Clique.html", "/Mulearn.html", "/Film Club.html",
                    "/Music Club.html", "/Velosters.html",
                    "/Break through science society.html"
                ).hasAnyRole("USER", "ADMIN")

                // Admin panel HTML pages — ADMIN role required
                .requestMatchers(
                    "/Index(admin).html",
                    "/**admin**.html"
                ).hasRole("ADMIN")

                // Admin API write operations require ADMIN role
                .requestMatchers(HttpMethod.POST, "/api/announcements/add").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/events/add").hasRole("ADMIN")

                // All other /api/admin/** require ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Static assets bypass Spring Security entirely — no filter chain overhead.
     * This is the single, canonical place for static-resource exclusions.
     */
@Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                // Static assets only — NOT HTML pages (those go through the filter chain)
                "/**/*.css",
                "/**/*.js",
                "/**/*.png",
                "/**/*.jpg",
                "/**/*.jpeg",
                "/**/*.gif",
                "/**/*.svg",
                "/**/*.ico",
                "/**/*.mp4",
                "/intro.mp4",
                "/Favicon.png",
                "/static/**",
                "/webjars/**"
        );
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Origins loaded from app.cors.allowed-origins (application.yml / env var)
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}