package com.habitforge.habitforge_backend.service;

import com.habitforge.habitforge_backend.dto.HabitDTO;
import com.habitforge.habitforge_backend.dto.HabitEditDTO;
import com.habitforge.habitforge_backend.model.Habit;
import com.habitforge.habitforge_backend.model.User;
import com.habitforge.habitforge_backend.repository.HabitRepository;
import com.habitforge.habitforge_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HabitService {

    private final HabitRepository habitRepo;
    private final UserRepository userRepo;
    private final HabitReminderService habitReminderService;

    public HabitService(
        HabitRepository habitRepo,
        UserRepository userRepo,
        HabitReminderService habitReminderService
    ) {
        this.habitRepo = habitRepo;
        this.userRepo = userRepo;
        this.habitReminderService = habitReminderService;
    }

    public HabitDTO createHabit(String username, String title, int targetDays) {
        User user = userRepo.findByUsername(username.trim());
        if (user == null) return null;

        Habit habit = new Habit();
        habit.setUser(user);
        habit.setTitle(title.trim());
        habit.setTargetDays(targetDays);
        habit.setStartDate(LocalDate.now());
        habit.setCurrentStreak(0);
        habit.setCompleted(false);

        Habit saved = habitRepo.save(habit);
        return convertToDTO(saved);
    }

    public List<HabitDTO> getUserHabits(String username) {
        User user = userRepo.findByUsername(username.trim());
        if (user == null) return List.of();

        return habitRepo.findByUser(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public boolean checkInHabit(String username, Long habitId) {
        User user = userRepo.findByUsername(username.trim());
        if (user == null) return false;

        Habit habit = habitRepo.findById(habitId).orElse(null);
        if (habit == null || !habit.getUser().equals(user)) return false;

        LocalDate today = LocalDate.now();

        // Prevent double check-in for today
        if (today.equals(habit.getLastCheckInDate())) {
            return false;
        }

        habit.getCompletedDays().add(today);

        LocalDate yesterday = today.minusDays(1);
        boolean maintainedStreak = yesterday.equals(habit.getLastCheckInDate());

        int newStreak = maintainedStreak ? habit.getCurrentStreak() + 1 : 1;
        habit.setCurrentStreak(newStreak);
        habit.setLastCheckInDate(today);

        // Mark as completed if streak goal met
        if (newStreak >= habit.getTargetDays()) {
            habit.setCompleted(true);
        }

        habitRepo.save(habit);
        return true;
    }

    public boolean deleteHabit(String username, Long habitId) {
        User user = userRepo.findByUsername(username.trim());
        if (user == null) return false;

        Habit habit = habitRepo.findById(habitId).orElse(null);
        if (habit == null || !habit.getUser().equals(user)) return false;

        habitReminderService.deleteReminderIfExists(habit); // also delete any reminders
        habitRepo.delete(habit);
        return true;
    }

    // ------------------ Edit Habit ------------------
    public boolean editHabit(String username, HabitEditDTO dto) {
        User user = userRepo.findByUsername(username.trim());
        if (user == null) return false;

        Habit habit = habitRepo.findById(dto.getHabitId()).orElse(null);
        if (habit == null || !habit.getUser().equals(user)) return false;

        habit.setTitle(dto.getTitle().trim());
        habit.setTargetDays(dto.getTargetDays());
        habit.setCompleted(dto.isCompleted());

        // Reminder logic
        String reminderStr = dto.getReminderTime();
        if (reminderStr != null && !reminderStr.isBlank()) {
            habitReminderService.createOrUpdateReminder(habit, reminderStr);
        } else {
            habitReminderService.deleteReminderIfExists(habit);
        }

        habitRepo.save(habit);
        return true;
    }

    // Overload editHabit to use habitId as a separate parameter
    public boolean editHabit(String username, Long habitId, HabitEditDTO dto) {
        dto.setHabitId(habitId);
        return editHabit(username, dto);
    }

    // ------------------ Sorting Support ------------------
    public List<HabitDTO> getUserHabitsSorted(String username, String sortBy) {
        User user = userRepo.findByUsername(username.trim());
        if (user == null) return List.of();

        List<Habit> habits = habitRepo.findByUser(user);

        Comparator<Habit> comparator;
        switch (sortBy.toLowerCase()) {
            case "startdate":
                comparator = Comparator.comparing(Habit::getStartDate);
                break;
            case "streak":
                comparator = Comparator.comparingInt(Habit::getCurrentStreak).reversed();
                break;
            case "completed":
                comparator = Comparator.comparing(Habit::isCompleted).reversed();
                break;
            default:
                comparator = Comparator.comparing(Habit::getId); // default order
        }

        return habits.stream()
                .sorted(comparator)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<HabitDTO> getSortedHabits(String username, String sortBy, String order) {
        List<HabitDTO> habits = getUserHabitsSorted(username, sortBy);
        if ("desc".equalsIgnoreCase(order)) {
            Collections.reverse(habits);
        }
        return habits;
    }

    // ------------------ Reminder Support ------------------
    public boolean setHabitReminder(String username, Long habitId, String timeStr) {
        User user = userRepo.findByUsername(username.trim());
        if (user == null) return false;

        Habit habit = habitRepo.findById(habitId).orElse(null);
        if (habit == null || !habit.getUser().equals(user)) return false;

        if (timeStr != null && !timeStr.isBlank()) {
            habitReminderService.createOrUpdateReminder(habit, timeStr);
        } else {
            habitReminderService.deleteReminderIfExists(habit);
        }

        return true;
    }

    // ------------------ Helper: DTO conversion ------------------
    private HabitDTO convertToDTO(Habit habit) {
        return new HabitDTO(
                habit.getId(),
                habit.getTitle(),
                habit.getTargetDays(),
                habit.getStartDate(),
                habit.getCurrentStreak(),
                habit.getLastCheckInDate(),
                habit.isCompleted(),
                Set.copyOf(habit.getCompletedDays())
        );
    }
}





