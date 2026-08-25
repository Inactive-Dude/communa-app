package com.login.communa.Controller;

import com.login.communa.Entity.Admin;
import com.login.communa.Entity.Event;
import com.login.communa.Repository.AdminRepository;
import com.login.communa.Service.EventService;
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
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final AdminRepository adminRepository;

    public EventController(EventService service, AdminRepository adminRepository) {
        this.service = service;
        this.adminRepository = adminRepository;
    }

    /**
     * Add a new event.
     * Requires ROLE_ADMIN and verifies that the authenticated admin's assigned
     * clubName matches the clubName in the request body — preventing cross-club
     * write attacks where an admin for Club A posts events as Club B.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<?> addEvent(
            @RequestBody @NonNull Event event,
            Authentication authentication) {

        // Resolve the authenticated admin's assigned club from the database
        String adminEmail = authentication.getName();
        Optional<Admin> adminOpt = adminRepository.findByEmail(adminEmail);

        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin account not found"));
        }

        String adminClub = adminOpt.get().getClubName();

        // Enforce: admin can only post events for their own club
        if (!adminClub.equalsIgnoreCase(event.getClubName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error",
                            "You are not authorized to post events for club: "
                            + event.getClubName()));
        }

        return ResponseEntity.ok(service.addEvent(event));
    }

    /** Any authenticated user can read events. */
    @GetMapping("/club/{clubName}")
    public @NonNull List<Event> getEvents(@PathVariable @NonNull String clubName) {
        return service.getEventsByClub(clubName);
    }
}
