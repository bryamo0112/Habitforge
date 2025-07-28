package com.habitforge.habitforge_backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private int targetDays;

    private LocalDate startDate;

    private int currentStreak;

    private LocalDate lastCheckInDate; // NEW FIELD

    private boolean completed = false; // NEW FIELD

    @ElementCollection
    private Set<LocalDate> completedDays = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
    private HabitReminder reminder;

    // --- Getters and Setters ---

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getTargetDays() { return targetDays; }
    public void setTargetDays(int targetDays) { this.targetDays = targetDays; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public LocalDate getLastCheckInDate() { return lastCheckInDate; }
    public void setLastCheckInDate(LocalDate lastCheckInDate) { this.lastCheckInDate = lastCheckInDate; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Set<LocalDate> getCompletedDays() { return completedDays; }
    public void setCompletedDays(Set<LocalDate> completedDays) { this.completedDays = completedDays; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public HabitReminder getReminder() { return reminder; }
    public void setReminder(HabitReminder reminder) { 
        this.reminder = reminder;
        if (reminder != null) {
            reminder.setHabit(this); // maintain bidirectional consistency
        }
    }
}




