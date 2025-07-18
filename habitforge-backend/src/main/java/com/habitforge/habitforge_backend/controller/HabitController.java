package com.habitforge.habitforge_backend.controller;

import com.habitforge.habitforge_backend.dto.HabitDTO;
import com.habitforge.habitforge_backend.service.HabitService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
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

    public record HabitCreateRequest(String title, int targetDays) {}
}


