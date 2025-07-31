package com.habitforge.habitforge_backend.service;

import com.habitforge.habitforge_backend.model.Habit;
import com.habitforge.habitforge_backend.model.HabitReminder;
import com.habitforge.habitforge_backend.repository.HabitReminderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Optional;

@Service
public class HabitReminderService {

    private final HabitReminderRepository reminderRepo;

    public HabitReminderService(HabitReminderRepository reminderRepo) {
        this.reminderRepo = reminderRepo;
    }

    public void createOrUpdateReminder(Habit habit, String reminderTimeStr) {
        // Parse and truncate to minute precision (strip seconds & nanos)
        LocalTime reminderTime = LocalTime.parse(reminderTimeStr)
                .withSecond(0)
                .withNano(0);

        Long habitId = habit.getId();
        Optional<HabitReminder> existing = reminderRepo.findByHabitId(habitId);

        if (existing.isPresent()) {
            HabitReminder reminder = existing.get();
            reminder.setReminderTime(reminderTime);
            reminder.setEnabled(true); // Ensure it's active
            reminderRepo.save(reminder);
        } else {
            HabitReminder reminder = new HabitReminder();
            reminder.setHabit(habit);
            reminder.setReminderTime(reminderTime);
            reminder.setEnabled(true); // New reminders should be enabled
            reminderRepo.save(reminder);
        }
    }

    public void deleteReminderIfExists(Habit habit) {
        Long habitId = habit.getId();
        reminderRepo.findByHabitId(habitId).ifPresent(reminderRepo::delete);
    }

    public Optional<HabitReminder> getReminderForHabit(Long habitId) {
        return reminderRepo.findByHabitId(habitId);
    }
}





