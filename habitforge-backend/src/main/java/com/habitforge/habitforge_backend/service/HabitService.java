package com.habitforge.habitforge_backend.service;

import com.habitforge.habitforge_backend.dto.HabitDTO;
import com.habitforge.habitforge_backend.model.Habit;
import com.habitforge.habitforge_backend.model.User;
import com.habitforge.habitforge_backend.repository.HabitRepository;
import com.habitforge.habitforge_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HabitService {

    private final HabitRepository habitRepo;
    private final UserRepository userRepo;

    public HabitService(HabitRepository habitRepo, UserRepository userRepo) {
        this.habitRepo = habitRepo;
        this.userRepo = userRepo;
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
        if (!habit.getCompletedDays().contains(today)) {
            habit.getCompletedDays().add(today);

            LocalDate yesterday = today.minusDays(1);
            boolean maintainedStreak = habit.getCompletedDays().contains(yesterday);

            habit.setCurrentStreak(maintainedStreak ? habit.getCurrentStreak() + 1 : 1);
            habitRepo.save(habit);
            return true;
        }
        return false;
    }

    private HabitDTO convertToDTO(Habit habit) {
        return new HabitDTO(
                habit.getId(),
                habit.getTitle(),
                habit.getTargetDays(),
                habit.getStartDate(),
                habit.getCurrentStreak(),
                Set.copyOf(habit.getCompletedDays())
        );
    }
}


