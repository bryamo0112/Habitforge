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

            if (trimmedUsername.isEmpty() || trimmedPassword.isEmpty()) return false;

            if (userRepository.findByUsername(trimmedUsername).isPresent()) return false;
            if (trimmedEmail != null && !trimmedEmail.isEmpty() && userRepository.findByEmail(trimmedEmail).isPresent()) return false;

            User user = new User();
            user.setUsername(trimmedUsername);
            user.setPassword(passwordEncoder.encode(trimmedPassword));
            user.setEmail(trimmedEmail);
            user.setEmailVerified(false);
            user.setVerificationCode(UUID.randomUUID().toString());
            user.setHasBeenPromptedForProfilePic(false);

            userRepository.save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public User authenticateUser(String usernameOrEmail, String rawPassword) {
        if (usernameOrEmail == null || rawPassword == null) return null;

        String input = usernameOrEmail.trim();
        String password = rawPassword.trim();

        Optional<User> optionalUser = input.contains("@")
            ? userRepository.findByEmail(input)
            : userRepository.findByUsername(input);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(password, user.getPassword())) return user;
        }
        return null;
    }

    public User findByUsername(String username) {
        return username == null ? null : userRepository.findByUsername(username.trim()).orElse(null);
    }

    public User findByEmail(String email) {
        return email == null ? null : userRepository.findByEmail(email.trim()).orElse(null);
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
        return username == null ? null : userRepository.findByUsername(username.trim()).map(User::getProfilePicture).orElse(null);
    }

    public String getProfilePictureContentType(String username) {
        return username == null ? null : userRepository.findByUsername(username.trim()).map(User::getProfilePictureContentType).orElse(null);
    }

    public boolean markUserPrompted(String username) {
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
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (code.equals(user.getVerificationCode())) {
                user.setEmailVerified(true);
                user.setVerificationCode(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public boolean generateAndSendPasswordResetCode(String email, String resetCode) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setResetPasswordCode(resetCode);
            userRepository.save(user);
            return true;
        }
        return false;
    }
    // Verify password reset code matches stored resetPasswordCode for the user
public boolean verifyResetCode(String email, String code) {
    Optional<User> userOpt = userRepository.findByEmail(email.trim());
    if (userOpt.isPresent()) {
        User user = userOpt.get();
        return code != null && code.equals(user.getResetPasswordCode());
    }
    return false;
}


    @Transactional
    public boolean resetPassword(String email, String code, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (code.equals(user.getResetPasswordCode())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetPasswordCode(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public boolean existsByEmail(String email) {
        return email != null && userRepository.existsByEmail(email.trim());
    }

    @Transactional
    public boolean updatePasswordByEmail(String email, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public void saveVerificationCode(String email, String code) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        userOpt.ifPresent(user -> {
            user.setVerificationCode(code);
            userRepository.save(user);
        });
    }

    public String generateLoginVerificationCode(String email) {
    Optional<User> userOpt = userRepository.findByEmail(email.trim());
    if (userOpt.isPresent()) {
        String code = String.format("%06d", (int)(Math.random() * 1000000));
        User user = userOpt.get();
        user.setLoginVerificationCode(code);
        userRepository.save(user);
        System.out.println("[DEBUG] Generated login code for " + email + ": " + code);
        return code;
    }
    return null;
}


    public boolean verifyLoginCode(String email, String code) {
    Optional<User> userOpt = userRepository.findByEmail(email.trim());
    if (userOpt.isPresent()) {
        User user = userOpt.get();
        System.out.println("[DEBUG] Verifying code for " + email + ": stored=" + user.getLoginVerificationCode() + ", input=" + code);
        if (code.trim().equals(user.getLoginVerificationCode())) {
            user.setLoginVerificationCode(null);
            user.setEmailVerified(true);
            userRepository.save(user);
            return true;
        }
    }
    return false;
}




    public void clearLoginVerificationCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        userOpt.ifPresent(user -> {
            user.setLoginVerificationCode(null);
            userRepository.save(user);
        });
    }

    @Transactional
    public User createUserFromEmailLogin(String email) {
        if (email == null || email.trim().isEmpty()) return null;
        String trimmedEmail = email.trim();

        return userRepository.findByEmail(trimmedEmail).orElseGet(() -> {
            User user = new User();
            user.setEmail(trimmedEmail);
            user.setEmailVerified(true);
            user.setHasBeenPromptedForProfilePic(false);
            // assign temporary username:
            user.setUsername("user_" + UUID.randomUUID().toString().substring(0,8));
            userRepository.save(user);
            return user;
        });
    }

    @Transactional
    public boolean assignUsernameToEmailUser(String email, String newUsername) {
        if (email == null || newUsername == null) return false;

        String trimmedEmail = email.trim();
        String trimmedUsername = newUsername.trim();

        if (userRepository.existsByUsername(trimmedUsername)) return false;

        Optional<User> userOpt = userRepository.findByEmail(trimmedEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                user.setUsername(trimmedUsername);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public boolean userHasEmail(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        return userOpt.map(u -> u.getEmail() != null && !u.getEmail().isEmpty()).orElse(false);
    }

    @Transactional
    public boolean addAndVerifyEmailForUser(String username, String email, String code) {
        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (userRepository.existsByEmail(email.trim())) return false;
            if (code.equals(user.getVerificationCode())) {
                user.setEmail(email.trim());
                user.setEmailVerified(true);
                user.setVerificationCode(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public boolean updateEmail(String username, String email) {
        if (email == null || email.trim().isEmpty()) return false;

        Optional<User> existing = userRepository.findByEmail(email.trim());
        if (existing.isPresent() && !existing.get().getUsername().equals(username)) {
            return false; // Email already used by another user
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        user.setEmail(email.trim());
        user.setEmailVerified(false); // Require verification again
        userRepository.save(user);
        return true;
    }

    public boolean markEmailVerified(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        user.setEmailVerified(true);
        userRepository.save(user);
        return true;
    }

    // Existing updateUsername method (keep this!)
    public boolean updateUsername(String currentUsername, String newUsername) {
        if (newUsername == null || newUsername.trim().isEmpty()) return false;

        Optional<User> existing = userRepository.findByUsername(newUsername.trim());
        if (existing.isPresent()) return false; // Username taken

        Optional<User> userOpt = userRepository.findByUsername(currentUsername.trim());
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        user.setUsername(newUsername.trim());
        userRepository.save(user);
        return true;
    }

    // **New overloaded updateUsername method**
    public boolean updateUsername(User user, String newUsername) {
        if (newUsername == null || newUsername.trim().isEmpty()) return false;

        if (userRepository.findByUsername(newUsername.trim()).isPresent()) {
            return false; // Username taken
        }

        user.setUsername(newUsername.trim());
        userRepository.save(user);
        return true;
    }

    public User getCurrentUser(String usernameOrEmail) {
        if (usernameOrEmail == null) return null;
        return usernameOrEmail.contains("@")
            ? findByEmail(usernameOrEmail)
            : findByUsername(usernameOrEmail);
    }
}

















