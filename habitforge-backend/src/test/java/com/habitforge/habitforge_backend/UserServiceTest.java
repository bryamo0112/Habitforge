package com.habitforge.habitforge_backend;

import com.habitforge.habitforge_backend.service.UserService;
import com.habitforge.habitforge_backend.model.User;
import com.habitforge.habitforge_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUsername("testuser");
        sampleUser.setPassword("encodedpassword");
        sampleUser.setEmail("test@example.com");
        sampleUser.setEmailVerified(false);
        sampleUser.setVerificationCode(UUID.randomUUID().toString());
    }

    @Test
    void registerUser_Success() {
        String username = "newuser";
        String password = "mypassword";
        String email = "newuser@example.com";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encodedpwd");

        boolean result = userService.registerUser(username, password, email);
        assertTrue(result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(username, savedUser.getUsername());
        assertEquals("encodedpwd", savedUser.getPassword());
        assertEquals(email, savedUser.getEmail());
        assertFalse(savedUser.isEmailVerified());
        assertNotNull(savedUser.getVerificationCode());
        assertFalse(savedUser.isHasBeenPromptedForProfilePic());
    }

    @Test
    void registerUser_FailsIfUsernameExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

        boolean result = userService.registerUser("testuser", "pass", "other@example.com");
        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_FailsIfEmailExists() {
        when(userRepository.findByUsername("uniqueuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        boolean result = userService.registerUser("uniqueuser", "pass", "test@example.com");
        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateUser_Success_ByUsername() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("rawpass", sampleUser.getPassword())).thenReturn(true);

        User user = userService.authenticateUser("testuser", "rawpass");
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
    }

    @Test
    void authenticateUser_Success_ByEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("rawpass", sampleUser.getPassword())).thenReturn(true);

        User user = userService.authenticateUser("test@example.com", "rawpass");
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
    }

    @Test
    void authenticateUser_Failure_WrongPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpass", sampleUser.getPassword())).thenReturn(false);

        User user = userService.authenticateUser("testuser", "wrongpass");
        assertNull(user);
    }

    @Test
    void authenticateUser_Failure_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        User user = userService.authenticateUser("unknown", "pass");
        assertNull(user);
    }

    @Test
    void findByUsername_Found() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        User user = userService.findByUsername("testuser");
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
    }

    @Test
    void findByUsername_NotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
        User user = userService.findByUsername("missing");
        assertNull(user);
    }

    @Test
    void saveProfilePicture_Success() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(mockFile.getBytes()).thenReturn("fakebytes".getBytes());
        when(mockFile.getContentType()).thenReturn("image/png");

        boolean result = userService.saveProfilePicture("testuser", mockFile);
        assertTrue(result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertArrayEquals("fakebytes".getBytes(), savedUser.getProfilePicture());
        assertEquals("image/png", savedUser.getProfilePictureContentType());
    }

    @Test
    void saveProfilePicture_Fail_UserNotFound() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        boolean result = userService.saveProfilePicture("missing", mockFile);
        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getProfilePicture_ReturnsBytes() {
        sampleUser.setProfilePicture("picdata".getBytes());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

        byte[] result = userService.getProfilePicture("testuser");
        assertArrayEquals("picdata".getBytes(), result);
    }

    @Test
    void getProfilePicture_ReturnsNullIfUserNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        byte[] result = userService.getProfilePicture("missing");
        assertNull(result);
    }

    @Test
    void markUserPrompted_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

        boolean result = userService.markUserPrompted("testuser");
        assertTrue(result);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().isHasBeenPromptedForProfilePic());
    }

    @Test
    void markUserPrompted_Fail_UserNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        boolean result = userService.markUserPrompted("missing");
        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmailCode_Success() {
        String code = "verify123";
        sampleUser.setVerificationCode(code);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        boolean result = userService.verifyEmailCode("test@example.com", code);
        assertTrue(result);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().isEmailVerified());
        assertNull(captor.getValue().getVerificationCode());
    }

    @Test
    void verifyEmailCode_Fail_WrongCode() {
        sampleUser.setVerificationCode("correctcode");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        boolean result = userService.verifyEmailCode("test@example.com", "wrongcode");
        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmailCode_Fail_UserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        boolean result = userService.verifyEmailCode("missing@example.com", "code");
        assertFalse(result);
    }

    
}
