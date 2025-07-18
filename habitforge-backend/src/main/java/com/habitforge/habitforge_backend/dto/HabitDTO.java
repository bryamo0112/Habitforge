package com.habitforge.habitforge_backend.dto;

import java.time.LocalDate;
import java.util.Set;

public class HabitDTO {
    private Long id;
    private String title;
    private int targetDays;
    private LocalDate startDate;
    private int currentStreak;
    private Set<LocalDate> completedDays;

    public HabitDTO() {}

    public HabitDTO(Long id, String title, int targetDays, LocalDate startDate, int currentStreak, Set<LocalDate> completedDays) {
        this.id = id;
        this.title = title;
        this.targetDays = targetDays;
        this.startDate = startDate;
        this.currentStreak = currentStreak;
        this.completedDays = completedDays;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getTargetDays() { return targetDays; }
    public void setTargetDays(int targetDays) { this.targetDays = targetDays; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public Set<LocalDate> getCompletedDays() { return completedDays; }
    public void setCompletedDays(Set<LocalDate> completedDays) { this.completedDays = completedDays; }
}

