package com.login.communa.Controller;

import com.login.communa.Entity.Admin;
import com.login.communa.Entity.Announcement;
import com.login.communa.Repository.AdminRepository;
import com.login.communa.Service.AnnouncementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService service;
    private final AdminRepository adminRepository;

    public AnnouncementController(AnnouncementService service, AdminRepository adminRepository) {
        this.service = service;
        this.adminRepository = adminRepository;
    }

    /**
     * Post a new announcement.
     * Requires ROLE_ADMIN and verifies that the authenticated admin's assigned
     * clubName matches the clubName in the request body — preventing cross-club
     * write attacks where an admin for Club A posts as Club B.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<?> addAnnouncement(
            @RequestBody @NonNull Announcement announcement,
            Authentication authentication) {

        // Resolve the authenticated admin's assigned club from the database
        String adminEmail = authentication.getName();
        Optional<Admin> adminOpt = adminRepository.findByEmail(adminEmail);

        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin account not found"));
        }

        String adminClub = adminOpt.get().getClubName();

        // Enforce: admin can only post to their own club
        if (!adminClub.equalsIgnoreCase(announcement.getClubName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error",
                            "You are not authorized to post announcements for club: "
                            + announcement.getClubName()));
        }

        return ResponseEntity.ok(service.addAnnouncement(announcement));
    }

    /** Any authenticated user can read announcements. */
    @GetMapping("/club/{clubName}")
    public @NonNull List<Announcement> getAnnouncements(@PathVariable @NonNull String clubName) {
        return service.getAnnouncementsByClub(clubName);
    }
}
