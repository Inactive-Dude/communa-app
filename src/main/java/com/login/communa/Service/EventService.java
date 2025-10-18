package com.login.communa.Service;

import com.login.communa.Entity.Event;
import com.login.communa.Repository.EventRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EventService {

    private final EventRepository repo;

    public EventService(EventRepository repo) { this.repo = repo; }

    public Event addEvent(Event event) { return repo.save(event); }

    public List<Event> getEventsByClub(String clubName) { return repo.findByClubNameOrderByDateAsc(clubName); }
}
