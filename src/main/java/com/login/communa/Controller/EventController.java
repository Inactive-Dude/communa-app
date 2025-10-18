package com.login.communa.Controller;

import com.login.communa.Entity.Event;
import com.login.communa.Service.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService service;

    public EventController(EventService service) { this.service = service; }

    @PostMapping("/add")
    public Event addEvent(@RequestBody Event event) { return service.addEvent(event); }

    @GetMapping("/club/{clubName}")
    public List<Event> getEvents(@PathVariable String clubName) { return service.getEventsByClub(clubName); }
}
