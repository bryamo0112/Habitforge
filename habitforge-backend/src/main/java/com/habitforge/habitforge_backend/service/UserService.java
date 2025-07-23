package com.habitforge.habitforge_backend.service;

import com.habitforge.habitforge_backend.model.User;
import com.habitforge.habitforge_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean registerUser(String username, String plainPassword, String email) {
        try {
            String trimmedUsername = username == null ? "" : username.trim();
            String trimmedPassword = plainPassword == null ? "" : plainPassword.trim();
            String trimmedEmail = (email == null) ? null : email.trim();

            if (trimmedUsername.isEmpty() || trimmedPassword.isEmpty()) {
                return false;
            }

            if (userRepository.findByUsername(trimmedUsername).isPresent()) {
                return false; // Username taken
            }

            if (trimmedEmail != null && !trimmedEmail.isEmpty() && userRepository.findByEmail(trimmedEmail).isPresent()) {
                return false; // Email taken
            }

            User user = new User();
            user.setUsername(trimmedUsername);
            user.setPassword(passwordEncoder.encode(trimmedPassword));
            user.setHasBeenPromptedForProfilePic(false);
            user.setEmail(trimmedEmail);
            user.setEmailVerified(false);
            user.setVerificationCode(UUID.randomUUID().toString());

            userRepository.save(user);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public User authenticateUser(String usernameOrEmail, String rawPassword) {
        if (usernameOrEmail == null || rawPassword == null) return null;

        String trimmedInput = usernameOrEmail.trim();
        String trimmedPassword = rawPassword.trim();

        Optional<User> optionalUser;
        if (trimmedInput.contains("@")) {
            optionalUser = userRepository.findByEmail(trimmedInput);
        } else {
            optionalUser = userRepository.findByUsername(trimmedInput);
        }

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(trimmedPassword, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    public User findByUsername(String username) {
        if (username == null) return null;
        return userRepository.findByUsername(username.trim()).orElse(null);
    }

    public User findByEmail(String email) {
        if (email == null) return null;
        return userRepository.findByEmail(email.trim()).orElse(null);
    }

    public boolean saveProfilePicture(String username, MultipartFile file) throws Exception {
        if (username == null || file == null) return false;

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        user.setProfilePicture(file.getBytes());
        user.setProfilePictureContentType(file.getContentType());
        userRepository.save(user);
        return true;
    }

    public byte[] getProfilePicture(String username) {
        if (username == null) return null;
        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        return userOpt.map(User::getProfilePicture).orElse(null);
    }

    public String getProfilePictureContentType(String username) {
        if (username == null) return null;
        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        return userOpt.map(User::getProfilePictureContentType).orElse(null);
    }

    public boolean markUserPrompted(String username) {
        if (username == null) return false;
        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setHasBeenPromptedForProfilePic(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean verifyEmailCode(String email, String code) {
        if (email == null || code == null) return false;
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getVerificationCode() != null && user.getVerificationCode().equals(code)) {
                user.setEmailVerified(true);
                user.setVerificationCode(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public boolean generateAndSendPasswordResetCode(String email, String resetCode) {
        if (email == null || resetCode == null) return false;
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setResetPasswordCode(resetCode);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean resetPassword(String email, String code, String newPassword) {
        if (email == null || code == null || newPassword == null) return false;
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getResetPasswordCode() != null && user.getResetPasswordCode().equals(code)) {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetPasswordCode(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return userRepository.existsByEmail(email.trim());
    }

    @Transactional
    public boolean updatePasswordByEmail(String email, String newPassword) {
        if (email == null || newPassword == null) return false;
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // ====== LOGIN VERIFICATION CODE SUPPORT ======

    // Save email verification code for email verification
    public void saveVerificationCode(String email, String code) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setVerificationCode(code);
            userRepository.save(user);
        }
    }

    // Generate login verification code, save to user, and return the code
    public String generateLoginVerificationCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String code = String.format("%06d", (int)(Math.random() * 1000000));
            user.setLoginVerificationCode(code);
            userRepository.save(user);
            return code;
        }
        return null;
    }

    // Verify login code matches for the user and clear it on success
    public boolean verifyLoginCode(String email, String code) {
        if (email == null || code == null) return false;
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (code.equals(user.getLoginVerificationCode())) {
                user.setLoginVerificationCode(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    // Clear login verification code for user
    public void clearLoginVerificationCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLoginVerificationCode(null);
            userRepository.save(user);
        }
    }
}













