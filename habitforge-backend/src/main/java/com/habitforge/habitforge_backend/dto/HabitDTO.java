package com.habitforge.habitforge_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.Set;

public class HabitDTO {
    private Long id;
    private String title;
    private int targetDays;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    private int currentStreak;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate lastCheckInDate;

    private boolean completed;
    private Set<LocalDate> completedDays;

    private String reminderTime;  // NEW FIELD: "HH:mm" format, nullable

    public HabitDTO() {}

    public HabitDTO(Long id, String title, int targetDays, LocalDate startDate,
                    int currentStreak, LocalDate lastCheckInDate, boolean completed,
                    Set<LocalDate> completedDays, String reminderTime) {
        this.id = id;
        this.title = title;
        this.targetDays = targetDays;
        this.startDate = startDate;
        this.currentStreak = currentStreak;
        this.lastCheckInDate = lastCheckInDate;
        this.completed = completed;
        this.completedDays = completedDays;
        this.reminderTime = reminderTime;
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

    public LocalDate getLastCheckInDate() { return lastCheckInDate; }
    public void setLastCheckInDate(LocalDate lastCheckInDate) { this.lastCheckInDate = lastCheckInDate; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Set<LocalDate> getCompletedDays() { return completedDays; }
    public void setCompletedDays(Set<LocalDate> completedDays) { this.completedDays = completedDays; }

    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }
}
