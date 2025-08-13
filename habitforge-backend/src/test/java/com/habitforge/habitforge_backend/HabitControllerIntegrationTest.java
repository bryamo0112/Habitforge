package com.habitforge.habitforge_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitforge.habitforge_backend.controller.HabitController.HabitCreateRequest;
import com.habitforge.habitforge_backend.dto.HabitEditDTO;
import com.habitforge.habitforge_backend.model.User;
import com.habitforge.habitforge_backend.repository.HabitRepository;
import com.habitforge.habitforge_backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
public class HabitControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private HabitRepository habitRepository;

    private String jwtToken;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpassword";

    // Helper method to login and get JWT token (reuse your UserController login flow)
    private String loginAndGetJwt(String username, String password) throws Exception {
        Map<String, String> loginRequest = Map.of(
            "username", username,
            "password", password
        );

        String response = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var jsonNode = objectMapper.readTree(response);
        return jsonNode.get("token").asText();
    }

   @BeforeEach
void setup() throws Exception {
    // Clean habits first to avoid FK errors
    habitRepository.deleteAll();

    // Clean users
    userRepository.deleteAll();

    // Create a new user
    User user = new User();
    user.setUsername(TEST_USERNAME);
    user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
    // Optional: add roles if your app requires
    // user.setRoles(Set.of("ROLE_USER")); // adjust if you have roles in User model

    userRepository.saveAndFlush(user);  // flush to DB

    // Login and get JWT token for this user
    jwtToken = loginAndGetJwt(TEST_USERNAME, TEST_PASSWORD);
}


    

    @Test
    void createHabit_success() throws Exception {
        HabitCreateRequest request = new HabitCreateRequest("Read books", 10);

        mockMvc.perform(post("/api/habits/create")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Read books"))
            .andExpect(jsonPath("$.targetDays").value(10))
            .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void createHabit_invalidInput_returnsBadRequest() throws Exception {
        // Empty title
        HabitCreateRequest request = new HabitCreateRequest("", 5);

        mockMvc.perform(post("/api/habits/create")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        // targetDays <= 0
        HabitCreateRequest invalidDaysRequest = new HabitCreateRequest("Learn", 0);
        mockMvc.perform(post("/api/habits/create")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDaysRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getHabits_returnsList() throws Exception {
        // Create one habit first (you can create multiple if you want)
        HabitCreateRequest request = new HabitCreateRequest("Exercise", 7);
        mockMvc.perform(post("/api/habits/create")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/habits")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", not(empty())))
            .andExpect(jsonPath("$[0].title", not(emptyString())));
    }

    @Test
    void checkIn_success_andAlreadyCheckedIn() throws Exception {
        // Create habit first
        HabitCreateRequest request = new HabitCreateRequest("Meditate", 5);
        String response = mockMvc.perform(post("/api/habits/create")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        long habitId = objectMapper.readTree(response).get("id").asLong();

        // First check-in - success
        mockMvc.perform(post("/api/habits/" + habitId + "/check-in")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(content().string("Check-in successful!"));

        // Second check-in - already checked in
        mockMvc.perform(post("/api/habits/" + habitId + "/check-in")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Already checked in today."));
    }

    @Test
    void deleteHabit_success_andFailure() throws Exception {
        HabitCreateRequest request = new HabitCreateRequest("Jogging", 15);
        String response = mockMvc.perform(post("/api/habits/create")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        long habitId = objectMapper.readTree(response).get("id").asLong();

        // Delete existing habit - success
        mockMvc.perform(delete("/api/habits/" + habitId)
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(content().string("Habit deleted successfully."));

        // Delete non-existent or already deleted habit - failure
        mockMvc.perform(delete("/api/habits/" + habitId)
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Unable to delete habit."));
    }

    @Test
void editHabit_success_andFailure() throws Exception {
    HabitCreateRequest createRequest = new HabitCreateRequest("Study", 20);
    String response = mockMvc.perform(post("/api/habits/create")
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    long habitId = objectMapper.readTree(response).get("id").asLong();

    // Create and set fields with setters
    HabitEditDTO editDto = new HabitEditDTO();
    editDto.setTitle("Study Math");
    editDto.setTargetDays(25);
    editDto.setCompleted(false);
    editDto.setReminderTime("08:00");

    mockMvc.perform(put("/api/habits/" + habitId + "/edit")
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(editDto)))
        .andExpect(status().isOk())
        .andExpect(content().string("Habit updated."));

    // Invalid update (empty JSON object)
    mockMvc.perform(put("/api/habits/" + habitId + "/edit")
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Unable to update habit."));
}


    @Test
    void getSortedHabits_returnsSortedList() throws Exception {
        // Create multiple habits
        HabitCreateRequest req1 = new HabitCreateRequest("A Habit", 5);
        HabitCreateRequest req2 = new HabitCreateRequest("B Habit", 10);

        mockMvc.perform(post("/api/habits/create")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/habits/create")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/habits/sorted")
                .param("sortBy", "title")
                .param("order", "asc")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
            .andExpect(jsonPath("$[0].title", is("A Habit")));
    }

    @Test
void setAndGetReminder_success_andInvalidTime() throws Exception {
    HabitCreateRequest request = new HabitCreateRequest("Yoga", 30);
    String response = mockMvc.perform(post("/api/habits/create")
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    long habitId = objectMapper.readTree(response).get("id").asLong();

    // Set valid reminder
    mockMvc.perform(post("/api/habits/" + habitId + "/reminder")
            .header("Authorization", "Bearer " + jwtToken)
            .param("time", "09:30"))
        .andExpect(status().isOk())
        .andExpect(content().string("Reminder set."));

    // Get reminder (added Authorization header here)
    mockMvc.perform(get("/api/habits/" + habitId + "/reminder")
            .header("Authorization", "Bearer " + jwtToken))
        .andExpect(status().isOk())
        .andExpect(content().string("09:30"));

    // Set invalid reminder (empty time)
    mockMvc.perform(post("/api/habits/" + habitId + "/reminder")
            .header("Authorization", "Bearer " + jwtToken)
            .param("time", ""))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Invalid reminder time."));
}

}

