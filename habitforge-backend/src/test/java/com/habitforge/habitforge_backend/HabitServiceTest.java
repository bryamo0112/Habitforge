package com.habitforge.habitforge_backend;

import com.habitforge.habitforge_backend.dto.HabitDTO;
import com.habitforge.habitforge_backend.dto.HabitEditDTO;
import com.habitforge.habitforge_backend.model.Habit;
import com.habitforge.habitforge_backend.model.User;
import com.habitforge.habitforge_backend.repository.HabitRepository;
import com.habitforge.habitforge_backend.repository.UserRepository;
import com.habitforge.habitforge_backend.service.HabitReminderService;
import com.habitforge.habitforge_backend.service.HabitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HabitServiceTest {

    HabitRepository habitRepo;
    UserRepository userRepo;
    HabitReminderService habitReminderService;

    HabitService habitService;

    User user;
    Habit habit;

    @BeforeEach
    void setUp() {
        habitRepo = mock(HabitRepository.class);
        userRepo = mock(UserRepository.class);
        habitReminderService = mock(HabitReminderService.class);

        habitService = new HabitService(habitRepo, userRepo, habitReminderService);

        user = new User();
        user.setUsername("testuser");

        habit = new Habit();
        setId(habit, 1L);
        habit.setUser(user);
        habit.setTitle("Test Habit");
        habit.setTargetDays(5);
        habit.setStartDate(LocalDate.now().minusDays(10));
        habit.setCurrentStreak(2);
        habit.setCompleted(false);
        habit.setCompletedDays(new HashSet<>());
    }

    private void setId(Habit habit, Long id) {
        try {
            Field field = Habit.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(habit, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set id on Habit", e);
        }
    }

    @Test
    void createHabit_ShouldReturnDTO_WhenUserExists() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.save(any())).thenAnswer(i -> {
            Habit h = i.getArgument(0);
            setId(h, 100L); // simulate DB generated id
            return h;
        });

        HabitDTO dto = habitService.createHabit("testuser", "New Habit", 7);

        assertNotNull(dto);
        assertEquals("New Habit", dto.getTitle());
        assertEquals(7, dto.getTargetDays());
        assertEquals(0, dto.getCurrentStreak());
        assertFalse(dto.isCompleted());
        assertEquals(user.getUsername(), user.getUsername());
        verify(habitRepo).save(any(Habit.class));
    }

    @Test
    void createHabit_ShouldReturnNull_WhenUserNotFound() {
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());
        HabitDTO dto = habitService.createHabit("unknown", "Habit", 3);
        assertNull(dto);
    }

    @Test
    void getUserHabits_ShouldReturnList_WhenUserExists() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findByUser(user)).thenReturn(List.of(habit));

        List<HabitDTO> habits = habitService.getUserHabits("testuser");

        assertEquals(1, habits.size());
        assertEquals(habit.getTitle(), habits.get(0).getTitle());
    }

    @Test
    void getUserHabits_ShouldReturnEmptyList_WhenUserNotFound() {
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());
        List<HabitDTO> habits = habitService.getUserHabits("unknown");
        assertTrue(habits.isEmpty());
    }

    @Test
    void checkInHabit_ShouldReturnTrue_WhenCheckInValid() {
        habit.setLastCheckInDate(LocalDate.now().minusDays(1));
        habit.setCurrentStreak(2);
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.of(habit));

        boolean result = habitService.checkInHabit("testuser", 1L);

        assertTrue(result);
        assertEquals(LocalDate.now(), habit.getLastCheckInDate());
        assertEquals(3, habit.getCurrentStreak());
        assertTrue(habit.getCompletedDays().contains(LocalDate.now()));
        verify(habitRepo).save(habit);
    }

    @Test
    void checkInHabit_ShouldReturnFalse_WhenAlreadyCheckedInToday() {
        habit.setLastCheckInDate(LocalDate.now());
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.of(habit));

        boolean result = habitService.checkInHabit("testuser", 1L);
        assertFalse(result);
        verify(habitRepo, never()).save(any());
    }

    @Test
    void checkInHabit_ShouldReturnFalse_WhenUserNotFound() {
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());
        boolean result = habitService.checkInHabit("unknown", 1L);
        assertFalse(result);
    }

    @Test
    void checkInHabit_ShouldReturnFalse_WhenHabitNotFound() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.empty());
        boolean result = habitService.checkInHabit("testuser", 1L);
        assertFalse(result);
    }

    @Test
    void checkInHabit_ShouldReturnFalse_WhenHabitNotBelongToUser() {
        User otherUser = new User();
        otherUser.setUsername("other");
        habit.setUser(otherUser);

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.of(habit));

        boolean result = habitService.checkInHabit("testuser", 1L);
        assertFalse(result);
    }

    @Test
    void deleteHabit_ShouldReturnTrue_WhenSuccessful() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.of(habit));

        boolean result = habitService.deleteHabit("testuser", 1L);

        assertTrue(result);
        verify(habitReminderService).deleteReminderIfExists(habit);
        verify(habitRepo).delete(habit);
    }

    @Test
    void deleteHabit_ShouldReturnFalse_WhenUserNotFound() {
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());
        boolean result = habitService.deleteHabit("unknown", 1L);
        assertFalse(result);
        verify(habitReminderService, never()).deleteReminderIfExists(any());
        verify(habitRepo, never()).delete(any());
    }

    @Test
    void deleteHabit_ShouldReturnFalse_WhenHabitNotFound() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.empty());
        boolean result = habitService.deleteHabit("testuser", 1L);
        assertFalse(result);
        verify(habitReminderService, never()).deleteReminderIfExists(any());
        verify(habitRepo, never()).delete(any());
    }

    @Test
    void deleteHabit_ShouldReturnFalse_WhenHabitNotBelongToUser() {
        User otherUser = new User();
        otherUser.setUsername("other");
        habit.setUser(otherUser);

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.of(habit));

        boolean result = habitService.deleteHabit("testuser", 1L);
        assertFalse(result);
        verify(habitReminderService, never()).deleteReminderIfExists(any());
        verify(habitRepo, never()).delete(any());
    }

    @Test
    void editHabit_ShouldReturnTrue_AndUpdateHabit() {
        HabitEditDTO dto = new HabitEditDTO();
        dto.setHabitId(1L);
        dto.setTitle("Updated Title");
        dto.setTargetDays(10);
        dto.setCompleted(true);
        dto.setReminderTime("09:30");

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.of(habit));

        boolean result = habitService.editHabit("testuser", dto);

        assertTrue(result);
        assertEquals("Updated Title", habit.getTitle());
        assertEquals(10, habit.getTargetDays());
        assertTrue(habit.isCompleted());
        verify(habitReminderService).createOrUpdateReminder(habit, "09:30");
        verify(habitRepo).save(habit);
    }

    @Test
    void editHabit_ShouldDeleteReminder_WhenReminderEmptyString() {
        HabitEditDTO dto = new HabitEditDTO();
        dto.setHabitId(1L);
        dto.setReminderTime("");

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.of(habit));

        boolean result = habitService.editHabit("testuser", dto);

        assertTrue(result);
        verify(habitReminderService).deleteReminderIfExists(habit);
        verify(habitRepo).save(habit);
    }

    @Test
    void editHabit_ShouldReturnFalse_WhenUserNotFound() {
        HabitEditDTO dto = new HabitEditDTO();
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());
        boolean result = habitService.editHabit("unknown", dto);
        assertFalse(result);
    }

    @Test
    void editHabit_ShouldReturnFalse_WhenHabitNotFound() {
        HabitEditDTO dto = new HabitEditDTO();
        dto.setHabitId(999L);
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(999L)).thenReturn(Optional.empty());

        boolean result = habitService.editHabit("testuser", dto);
        assertFalse(result);
    }

    @Test
    void getUserHabitsSorted_ShouldSortByStartDate() {
        Habit habit2 = new Habit();
        setId(habit2, 2L);
        habit2.setUser(user);
        habit2.setTitle("Second Habit");
        habit2.setStartDate(LocalDate.now().minusDays(5));

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findByUser(user)).thenReturn(List.of(habit, habit2));

        List<HabitDTO> sorted = habitService.getUserHabitsSorted("testuser", "startdate");

        assertEquals(2, sorted.size());
        assertEquals(habit.getId(), sorted.get(0).getId());
        assertEquals(habit2.getId(), sorted.get(1).getId());
    }

    @Test
    void getUserHabitsSorted_ShouldSortByStreakDescending() {
        Habit habit2 = new Habit();
        setId(habit2, 2L);
        habit2.setUser(user);
        habit2.setTitle("Second Habit");
        habit2.setCurrentStreak(5);

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findByUser(user)).thenReturn(List.of(habit, habit2));

        List<HabitDTO> sorted = habitService.getUserHabitsSorted("testuser", "streak");

        assertEquals(2, sorted.size());
        assertEquals(habit2.getId(), sorted.get(0).getId()); // highest streak first
    }

    @Test
    void getUserHabitsSorted_ShouldSortByCompletedDescending() {
        Habit habit2 = new Habit();
        setId(habit2, 2L);
        habit2.setUser(user);
        habit2.setTitle("Second Habit");
        habit2.setCompleted(true);

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findByUser(user)).thenReturn(List.of(habit, habit2));

        List<HabitDTO> sorted = habitService.getUserHabitsSorted("testuser", "completed");

        assertEquals(2, sorted.size());
        assertTrue(sorted.get(0).isCompleted());
    }

    @Test
void getSortedHabits_ShouldReverseWhenOrderDesc() {
    HabitService spyService = spy(habitService);

    HabitDTO dto1 = new HabitDTO(1L, "A", 3, LocalDate.now(), 1, null, false, Set.of(), null);
    HabitDTO dto2 = new HabitDTO(2L, "B", 5, LocalDate.now(), 3, null, false, Set.of(), null);

    // Use mutable list to avoid UnsupportedOperationException on reverse
    List<HabitDTO> habitsAsc = new ArrayList<>(List.of(dto1, dto2));

    doReturn(habitsAsc).when(spyService).getUserHabitsSorted("testuser", "title");

    List<HabitDTO> resultAsc = spyService.getSortedHabits("testuser", "title", "asc");
    assertEquals(dto1.getId(), resultAsc.get(0).getId());

    List<HabitDTO> resultDesc = spyService.getSortedHabits("testuser", "title", "desc");
    assertEquals(dto2.getId(), resultDesc.get(0).getId());
}



    @Test
    void setHabitReminder_ShouldCreateOrUpdate_WhenTimePresent() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.of(habit));

        boolean result = habitService.setHabitReminder("testuser", 1L, "08:30");

        assertTrue(result);
        verify(habitReminderService).createOrUpdateReminder(habit, "08:30");
    }

    @Test
    void setHabitReminder_ShouldDeleteReminder_WhenTimeBlank() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.of(habit));

        boolean result = habitService.setHabitReminder("testuser", 1L, "");

        assertTrue(result);
        verify(habitReminderService).deleteReminderIfExists(habit);
    }

    @Test
    void setHabitReminder_ShouldReturnFalse_WhenUserNotFound() {
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());
        boolean result = habitService.setHabitReminder("unknown", 1L, "09:00");
        assertFalse(result);
    }

    @Test
    void setHabitReminder_ShouldReturnFalse_WhenHabitNotFound() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.empty());
        boolean result = habitService.setHabitReminder("testuser", 1L, "09:00");
        assertFalse(result);
    }

    @Test
    void setHabitReminder_ShouldReturnFalse_WhenHabitNotBelongToUser() {
        User otherUser = new User();
        otherUser.setUsername("other");
        habit.setUser(otherUser);

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(habitRepo.findById(1L)).thenReturn(Optional.of(habit));

        boolean result = habitService.setHabitReminder("testuser", 1L, "09:00");
        assertFalse(result);
    }
}


