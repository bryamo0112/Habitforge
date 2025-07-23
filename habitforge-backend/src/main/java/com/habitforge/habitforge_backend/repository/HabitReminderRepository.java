package com.habitforge.habitforge_backend.repository;

import com.habitforge.habitforge_backend.model.HabitReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitReminderRepository extends JpaRepository<HabitReminder, Long> {

    Optional<HabitReminder> findByHabitId(Long habitId);

    // Fetch all enabled reminders
    List<HabitReminder> findByEnabledTrue();

    // Fetch all enabled reminders for a specific reminder time (used by scheduler)
    List<HabitReminder> findByReminderTimeAndEnabledTrue(LocalTime reminderTime);
}




