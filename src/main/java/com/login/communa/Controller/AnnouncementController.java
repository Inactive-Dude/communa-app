package com.login.communa.Controller;

import com.login.communa.Entity.Announcement;
import com.login.communa.Service.AnnouncementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "*") // allow frontend requests
public class AnnouncementController {

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    // Admin adds announcement
    @PostMapping("/add")
    public Announcement addAnnouncement(@RequestBody Announcement announcement) {
        return service.addAnnouncement(announcement);
    }

    // Students fetch announcements for a club
    @GetMapping("/club/{clubName}")
    public List<Announcement> getAnnouncements(@PathVariable String clubName) {
        return service.getAnnouncementsByClub(clubName);
    }
}
