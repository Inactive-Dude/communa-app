package com.login.communa.Service;

import com.login.communa.Entity.Event;
import com.login.communa.Repository.EventRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class EventService {

    private final EventRepository repo;

    public EventService(EventRepository repo) { this.repo = repo; }

    public @NonNull Event addEvent(@NonNull Event event) {
        // repo.save() returns @NonNull per Spring Data contract — guard for IDE null analysis.
        return Objects.requireNonNull(
            repo.save(event),
            "Saved event must not be null"
        );
    }

    public @NonNull List<Event> getEventsByClub(@NonNull String clubName) {
        return Objects.requireNonNull(
            repo.findByClubNameOrderByDateAsc(clubName),
            "Event list must not be null"
        );
    }
}
