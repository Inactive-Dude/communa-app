package com.login.communa.Repository;

import com.login.communa.Entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByClubNameOrderByDateAsc(String clubName);
}
