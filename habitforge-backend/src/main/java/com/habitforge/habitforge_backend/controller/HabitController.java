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

    @PostMapping("/create")
    public ResponseEntity<HabitDTO> createHabit(@RequestBody HabitCreateRequest request, Authentication auth) {
        String username = auth.getName();
        HabitDTO habit = habitService.createHabit(username, request.title(), request.targetDays());
        return (habit != null) ? ResponseEntity.ok(habit) : ResponseEntity.badRequest().build();
    }

    @GetMapping
    public ResponseEntity<List<HabitDTO>> getHabits(Authentication auth) {
        return ResponseEntity.ok(habitService.getUserHabits(auth.getName()));
    }

    @PostMapping("/{habitId}/check-in")
    public ResponseEntity<String> checkIn(@PathVariable Long habitId, Authentication auth) {
        boolean success = habitService.checkInHabit(auth.getName(), habitId);
        return success
                ? ResponseEntity.ok("Check-in successful!")
                : ResponseEntity.badRequest().body("Already checked in today or habit not found.");
    }

    @DeleteMapping("/{habitId}")
    public ResponseEntity<String> deleteHabit(@PathVariable Long habitId, Authentication auth) {
        boolean success = habitService.deleteHabit(auth.getName(), habitId);
        return success
                ? ResponseEntity.ok("Habit deleted successfully.")
                : ResponseEntity.badRequest().body("Unable to delete habit.");
    }

    // Edit habit (title, targetDays, reminderTime) - habitId in path
    @PutMapping("/{habitId}/edit")
    public ResponseEntity<String> editHabit(
            @PathVariable Long habitId,
            @RequestBody HabitEditDTO dto,
            Authentication auth) {
        boolean updated = habitService.editHabit(auth.getName(), habitId, dto);
        return updated
                ? ResponseEntity.ok("Habit updated.")
                : ResponseEntity.badRequest().body("Unable to update habit.");
    }

    // Sort habits
    @GetMapping("/sorted")
    public ResponseEntity<List<HabitDTO>> getSortedHabits(
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            Authentication auth) {
        return ResponseEntity.ok(habitService.getSortedHabits(auth.getName(), sortBy, order));
    }

    // Set or update reminder
    @PostMapping("/{habitId}/reminder")
    public ResponseEntity<String> setReminder(
            @PathVariable Long habitId,
            @RequestParam String time,
            Authentication auth) {
        boolean success = habitService.setHabitReminder(auth.getName(), habitId, time);
        return success
                ? ResponseEntity.ok("Reminder set.")
                : ResponseEntity.badRequest().body("Could not set reminder.");
    }

    // Get existing reminder
    @GetMapping("/{habitId}/reminder")
    public ResponseEntity<String> getReminder(@PathVariable Long habitId) {
        Optional<HabitReminder> reminder = reminderService.getReminderForHabit(habitId);
        return reminder.map(r ->
                ResponseEntity.ok(r.getReminderTime().toString())
        ).orElse(ResponseEntity.noContent().build());
    }

    public record HabitCreateRequest(String title, int targetDays) {}
}






