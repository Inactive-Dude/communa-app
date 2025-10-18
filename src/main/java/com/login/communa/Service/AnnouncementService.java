package com.login.communa.Service;

import com.login.communa.Entity.Announcement;
import com.login.communa.Repository.AnnouncementRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementService {
    private final AnnouncementRepository repo;

    public AnnouncementService(AnnouncementRepository repo) {
        this.repo = repo;
    }

    public Announcement addAnnouncement(Announcement announcement) {
        return repo.save(announcement);
    }

    public List<Announcement> getAnnouncementsByClub(String clubName) {
        return repo.findByClubNameOrderByPostedAtDesc(clubName);
    }
}
