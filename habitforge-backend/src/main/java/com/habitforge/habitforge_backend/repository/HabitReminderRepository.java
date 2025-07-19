package com.habitforge.habitforge_backend.repository;

import com.habitforge.habitforge_backend.model.HabitReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HabitReminderRepository extends JpaRepository<HabitReminder, Long> {
    Optional<HabitReminder> findByHabitId(Long habitId);
}

