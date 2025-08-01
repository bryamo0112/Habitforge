package com.habitforge.habitforge_backend.controller;

import com.habitforge.habitforge_backend.dto.HabitDTO;
import com.habitforge.habitforge_backend.dto.HabitEditDTO;
import com.habitforge.habitforge_backend.model.HabitReminder;
import com.habitforge.habitforge_backend.service.HabitReminderService;
import com.habitforge.habitforge_backend.service.HabitService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;
    private final HabitReminderService reminderService;

    public HabitController(HabitService habitService, HabitReminderService reminderService) {
        this.habitService = habitService;
        this.reminderService = reminderService;
    }

    // Create a new habit
    @PostMapping("/create")
    public ResponseEntity<HabitDTO> createHabit(@RequestBody HabitCreateRequest request, Authentication auth) {
        String username = auth.getName();
        if (request.title() == null || request.title().trim().isEmpty() || request.targetDays() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        HabitDTO habit = habitService.createHabit(username, request.title(), request.targetDays());
        return (habit != null) ? ResponseEntity.ok(habit) : ResponseEntity.badRequest().build();
    }

    // Get all habits for the logged-in user
    @GetMapping
    public ResponseEntity<List<HabitDTO>> getHabits(Authentication auth) {
        List<HabitDTO> habits = habitService.getUserHabits(auth.getName());
        return ResponseEntity.ok(habits);
    }

    // Check in to a habit for today
    @PostMapping("/{habitId}/check-in")
    public ResponseEntity<String> checkIn(@PathVariable Long habitId, Authentication auth) {
        boolean success = habitService.checkInHabit(auth.getName(), habitId);
        if (success) {
            return ResponseEntity.ok("Check-in successful!");
        } else {
            return ResponseEntity.badRequest().body("Already checked in today.");
        }
    }

    // Delete a habit by id
    @DeleteMapping("/{habitId}")
    public ResponseEntity<String> deleteHabit(@PathVariable Long habitId, Authentication auth) {
        boolean success = habitService.deleteHabit(auth.getName(), habitId);
        if (success) {
            return ResponseEntity.ok("Habit deleted successfully.");
        } else {
            return ResponseEntity.badRequest().body("Unable to delete habit.");
        }
    }

    // Edit habit fields (title, targetDays, completed, reminderTime)
    @PutMapping("/{habitId}/edit")
    public ResponseEntity<String> editHabit(
            @PathVariable Long habitId,
            @RequestBody HabitEditDTO dto,
            Authentication auth) {
        if (dto == null) {
            return ResponseEntity.badRequest().body("Invalid habit data.");
        }
        boolean updated = habitService.editHabit(auth.getName(), habitId, dto);
        if (updated) {
            return ResponseEntity.ok("Habit updated.");
        } else {
            return ResponseEntity.badRequest().body("Unable to update habit.");
        }
    }

    // Get habits sorted by a given field and order
    @GetMapping("/sorted")
    public ResponseEntity<List<HabitDTO>> getSortedHabits(
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            Authentication auth) {
        List<HabitDTO> sortedHabits = habitService.getSortedHabits(auth.getName(), sortBy, order);
        return ResponseEntity.ok(sortedHabits);
    }

    // Set or update reminder for a habit
    @PostMapping("/{habitId}/reminder")
    public ResponseEntity<String> setReminder(
            @PathVariable Long habitId,
            @RequestParam String time,
            Authentication auth) {
        if (time == null || time.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid reminder time.");
        }
        boolean success = habitService.setHabitReminder(auth.getName(), habitId, time);
        if (success) {
            return ResponseEntity.ok("Reminder set.");
        } else {
            return ResponseEntity.badRequest().body("Could not set reminder.");
        }
    }

    // Get existing reminder time for a habit
    @GetMapping("/{habitId}/reminder")
    public ResponseEntity<String> getReminder(@PathVariable Long habitId) {
        Optional<HabitReminder> reminderOpt = reminderService.getReminderForHabit(habitId);
        if (reminderOpt.isPresent()) {
            return ResponseEntity.ok(reminderOpt.get().getReminderTime().toString());
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    // Request record for habit creation payload
    public record HabitCreateRequest(String title, int targetDays) {}
}







