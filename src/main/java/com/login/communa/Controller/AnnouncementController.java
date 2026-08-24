package com.login.communa.Controller;

import com.login.communa.Entity.Announcement;
import com.login.communa.Service.AnnouncementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    /** Only admins (ROLE_ADMIN JWT) can post announcements. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public Announcement addAnnouncement(@RequestBody Announcement announcement) {
        return service.addAnnouncement(announcement);
    }

    /** Any authenticated user (or public if desired) can read announcements. */
    @GetMapping("/club/{clubName}")
    public List<Announcement> getAnnouncements(@PathVariable String clubName) {
        return service.getAnnouncementsByClub(clubName);
    }
}
