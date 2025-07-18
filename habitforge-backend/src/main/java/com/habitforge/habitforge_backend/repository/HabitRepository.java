package com.habitforge.habitforge_backend.repository;

import com.habitforge.habitforge_backend.model.Habit;
import com.habitforge.habitforge_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByUser(User user);
}

