package com.login.communa.Controller;

import com.login.communa.Entity.Event;
import com.login.communa.Service.EventService;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) { this.service = service; }

    /** Only admins (ROLE_ADMIN JWT) can post events. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public @NonNull Event addEvent(@RequestBody @NonNull Event event) {
        return service.addEvent(event);
    }

    /** Any authenticated user can read events. */
    @GetMapping("/club/{clubName}")
    public @NonNull List<Event> getEvents(@PathVariable @NonNull String clubName) {
        return service.getEventsByClub(clubName);
    }
}
