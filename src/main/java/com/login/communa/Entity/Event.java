package com.login.communa.Entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    /**
     * Mapped to the "event_date" column defined in init.sql.
     * Previously mapped as "date" which caused a silent schema mismatch
     * with the DBA-managed init.sql definition.
     */
    @Column(name = "event_date")
    private LocalDate date;

    private String time;
    private String location;

    @Column(name = "club_name")
    private String clubName;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }
}
