package com.habitforge.habitforge_backend.service;

import com.habitforge.habitforge_backend.model.User;
import com.habitforge.habitforge_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean registerUser(String username, String plainPassword) {
        try {
            String trimmedUsername = username == null ? "" : username.trim();
            String trimmedPassword = plainPassword == null ? "" : plainPassword.trim();

            System.out.println("Attempting to register user: " + trimmedUsername);

            if (trimmedUsername.isEmpty() || trimmedPassword.isEmpty()) {
                System.out.println("Username or password is empty after trimming.");
                return false;
            }

            if (userRepository.findByUsername(trimmedUsername) != null) {
                System.out.println("User already exists: " + trimmedUsername);
                return false;
            }

            User user = new User();
            user.setUsername(trimmedUsername);
            user.setPassword(passwordEncoder.encode(trimmedPassword));
            user.setHasBeenPromptedForProfilePic(false);

            userRepository.save(user);
            System.out.println("User saved successfully: " + trimmedUsername);
            return true;

        } catch (Exception e) {
            System.err.println("Error saving user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public User authenticateUser(String username, String rawPassword) {
        String trimmedUsername = username == null ? "" : username.trim();
        String trimmedPassword = rawPassword == null ? "" : rawPassword.trim();

        Optional<User> optionalUser = Optional.ofNullable(userRepository.findByUsername(trimmedUsername));
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(trimmedPassword, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username == null ? null : username.trim());
    }

    public boolean saveProfilePicture(String username, MultipartFile file) throws Exception {
        User user = userRepository.findByUsername(username.trim());
        if (user == null) return false;

        user.setProfilePicture(file.getBytes());
        user.setProfilePictureContentType(file.getContentType());
        userRepository.save(user);
        return true;
    }

    public byte[] getProfilePicture(String username) {
        User user = userRepository.findByUsername(username.trim());
        if (user == null || user.getProfilePicture() == null) {
            return null;
        }
        return user.getProfilePicture();
    }

    public String getProfilePictureContentType(String username) {
        User user = userRepository.findByUsername(username.trim());
        if (user == null || user.getProfilePictureContentType() == null) {
            return null;
        }
        return user.getProfilePictureContentType();
    }

    public boolean markUserPrompted(String username) {
        User user = userRepository.findByUsername(username.trim());
        if (user != null) {
            user.setHasBeenPromptedForProfilePic(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}








