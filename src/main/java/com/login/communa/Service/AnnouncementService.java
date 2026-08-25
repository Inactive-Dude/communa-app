package com.login.communa.Service;

import com.login.communa.Entity.Announcement;
import com.login.communa.Repository.AnnouncementRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AnnouncementService {

    private final AnnouncementRepository repo;

    public AnnouncementService(AnnouncementRepository repo) {
        this.repo = repo;
    }

    public @NonNull Announcement addAnnouncement(@NonNull Announcement announcement) {
        // repo.save() returns @NonNull per Spring Data contract — guard for IDE null analysis.
        return Objects.requireNonNull(
            repo.save(announcement),
            "Saved announcement must not be null"
        );
    }

    public @NonNull List<Announcement> getAnnouncementsByClub(@NonNull String clubName) {
        return Objects.requireNonNull(
            repo.findByClubNameOrderByPostedAtDesc(clubName),
            "Announcement list must not be null"
        );
    }
}
