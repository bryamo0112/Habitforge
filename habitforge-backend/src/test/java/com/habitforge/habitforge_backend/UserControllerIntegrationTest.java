package com.habitforge.habitforge_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitforge.habitforge_backend.model.User;
import com.habitforge.habitforge_backend.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration tests for UserController with real SecurityConfig + JwtFilter.
 * - Loads full Spring context (no mocks)
 * - Uses H2 test DB via "test" profile
 * - Logs in to get a real JWT and calls protected endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserService userService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "pass123";
    private static final String OTHER_USERNAME = "otheruser";

    private String loginAndGetJwt(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));
        String json = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode root = objectMapper.readTree(json);
        return root.get("token").asText();
    }

    @BeforeEach
    void seedUsers() {
        // Create one user WITHOUT an email so username/password login returns 200 (not 202)
        boolean created = userService.registerUser(TEST_USERNAME, TEST_PASSWORD, null);
        assertThat(created).isTrue();

        // A second user for username-uniqueness checks
        boolean other = userService.registerUser(OTHER_USERNAME, "secret", null);
        assertThat(other).isTrue();

        // Sanity: user exists
        User u = userService.findByUsername(TEST_USERNAME);
        assertThat(u).isNotNull();
    }

    // ---------------- Public endpoints ----------------

    @Test
    void signup_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "newuser",
                "password", "newpass",
                "email", "newuser@example.com"
        ));

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("User account successfully created."));
    }

    @Test
    void signup_fail_validation() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "",
                "password", "   "
        ));

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username and password cannot be empty."));
    }

    @Test
    void login_success_when_no_email_on_account() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", TEST_USERNAME,
                "password", TEST_PASSWORD
        ));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.emailVerified").value(false)); // no email set => false or null depending on defaults
    }

    @Test
    void send_verification_code_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "emailonly@example.com"));

        mockMvc.perform(post("/api/users/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("Verification code sent to email."));
    }

    @Test
    void verify_code_invalid_returns_400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "nobody@example.com",
                "code", "BADCODE"
        ));

        mockMvc.perform(post("/api/users/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired verification code."));
    }

    @Test
    void get_profile_picture_not_found() throws Exception {
        mockMvc.perform(get("/api/users/{username}/profile-picture", TEST_USERNAME))
                .andExpect(status().isNotFound());
    }

    // ---------------- Secured endpoints (require Bearer token) ----------------

    @Test
    void current_user_success() throws Exception {
        String jwt = loginAndGetJwt(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(get("/api/users/current")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.token").value(jwt));
    }

    @Test
    void set_email_success() throws Exception {
        String jwt = loginAndGetJwt(TEST_USERNAME, TEST_PASSWORD);

        String body = objectMapper.writeValueAsString(Map.of("email", "setme@example.com"));

        mockMvc.perform(post("/api/users/set-email")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("Email set successfully."));
    }

    @Test
    void set_username_success() throws Exception {
        String jwt = loginAndGetJwt(TEST_USERNAME, TEST_PASSWORD);

        // Ensure target username is not taken
        String newUsername = "renameduser";
        assertThat(userService.findByUsername(newUsername)).isNull();

        String body = objectMapper.writeValueAsString(Map.of("username", newUsername));

        mockMvc.perform(post("/api/users/set-username")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(newUsername))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void upload_profile_picture_success() throws Exception {
        String jwt = loginAndGetJwt(TEST_USERNAME, TEST_PASSWORD);

        MockMultipartFile file = new MockMultipartFile(
                "image", "pic.png", "image/png", "PNGDATA".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/users/{username}/upload-profile-picture", TEST_USERNAME)
                        .file(file)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.token").value(jwt));
    }

    @Test
    void upload_profile_picture_forbidden_when_username_mismatch() throws Exception {
        String jwt = loginAndGetJwt(TEST_USERNAME, TEST_PASSWORD);

        MockMultipartFile file = new MockMultipartFile(
                "image", "pic.png", "image/png", "PNGDATA".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/users/{username}/upload-profile-picture", OTHER_USERNAME)
                        .file(file)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void mark_prompted_success() throws Exception {
        String jwt = loginAndGetJwt(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(put("/api/users/{username}/mark-prompted", TEST_USERNAME)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    void forgot_password_ok_when_email_exists() throws Exception {
        // First set an email on the existing user
        String jwt = loginAndGetJwt(TEST_USERNAME, TEST_PASSWORD);
        mockMvc.perform(post("/api/users/set-email")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "resetme@example.com"))))
                .andExpect(status().isOk());

        // Now forgot-password should 200
        mockMvc.perform(post("/api/users/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "resetme@example.com"))))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset code sent to email."));
    }

    @Test
    void reset_password_fails_with_bad_code() throws Exception {
        // Make sure email exists
        String jwt = loginAndGetJwt(TEST_USERNAME, TEST_PASSWORD);
        mockMvc.perform(post("/api/users/set-email")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "resetbad@example.com"))))
                .andExpect(status().isOk());

        // Try reset with wrong code -> 400
        mockMvc.perform(post("/api/users/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "resetbad@example.com",
                                "code", "WRONG",
                                "newPassword", "NewPass1!"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or expired reset code."));
    }

    @Test
void logout_ok() throws Exception {
    String jwt = loginAndGetJwt(TEST_USERNAME, TEST_PASSWORD);

    mockMvc.perform(post("/api/users/logout")
            .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(content().string("Logout successful."));
}

}









