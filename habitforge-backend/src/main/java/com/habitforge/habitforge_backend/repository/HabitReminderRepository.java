package com.habitforge.habitforge_backend.repository;

import com.habitforge.habitforge_backend.model.HabitReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitReminderRepository extends JpaRepository<HabitReminder, Long> {

    Optional<HabitReminder> findByHabitId(Long habitId);

    List<HabitReminder> findByEnabledTrue();

    @Query("SELECT hr FROM HabitReminder hr " +
           "JOIN FETCH hr.habit h " +
           "JOIN FETCH h.user u " +
           "WHERE hr.enabled = true AND hr.reminderTime BETWEEN :start AND :end")
    List<HabitReminder> findDueRemindersWithHabitAndUser(
        @Param("start") LocalTime start,
        @Param("end") LocalTime end);
}





