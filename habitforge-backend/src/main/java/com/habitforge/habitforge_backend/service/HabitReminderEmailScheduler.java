package com.habitforge.habitforge_backend.service;

import com.habitforge.habitforge_backend.model.HabitReminder;
import com.habitforge.habitforge_backend.repository.HabitReminderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
public class HabitReminderEmailScheduler {

    private final HabitReminderRepository reminderRepository;
    private final EmailService emailService;

    public HabitReminderEmailScheduler(HabitReminderRepository reminderRepository, EmailService emailService) {
        this.reminderRepository = reminderRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 * * * * *") // Every minute at 0 seconds
    public void sendDueReminders() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0); // current time rounded to minute

        List<HabitReminder> dueReminders = reminderRepository.findByReminderTimeAndEnabledTrue(now);

        for (HabitReminder reminder : dueReminders) {
            var habit = reminder.getHabit();
            if (habit == null) continue;

            var user = habit.getUser();
            if (user == null) continue;

            String email = user.getEmail();
            if (email == null || email.isEmpty()) continue;

            String subject = "Habit Reminder: " + habit.getTitle();
            String content = String.format("""
                <p>Hi %s,</p>
                <p>This is your daily reminder to work on your habit: <strong>%s</strong>.</p>
                <p>Keep up the great work!</p>
                """, user.getUsername(), habit.getTitle());

            try {
                emailService.sendHtmlEmail(email, subject, content);
            } catch (Exception e) {
                System.err.println("Failed to send reminder email to " + email + ": " + e.getMessage());
            }
        }
    }
}


