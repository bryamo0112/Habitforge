package com.habitforge.habitforge_backend.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
public class HabitReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "habit_id", nullable = false, unique = true)
    private Habit habit;

    private LocalTime reminderTime; // E.g., 08:00 AM

    private boolean enabled = true;

    public HabitReminder() {
    }

    public HabitReminder(Habit habit, LocalTime reminderTime, boolean enabled) {
        this.habit = habit;
        this.reminderTime = reminderTime;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public Habit getHabit() {
        return habit;
    }

    public void setHabit(Habit habit) {
        this.habit = habit;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}


