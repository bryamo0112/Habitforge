package com.habitforge.habitforge_backend.controller;

import com.habitforge.habitforge_backend.model.User;
import com.habitforge.habitforge_backend.security.JwtUtil;
import com.habitforge.habitforge_backend.service.EmailService;
import com.habitforge.habitforge_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired private UserService userService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private EmailService emailService;
    @Autowired private AuthenticationManager authenticationManager;

    public static class UserDTO {
        public String username;
        public String password;
        public String email; // Optional for signup
    }

    public static class EmailVerificationDTO {
        public String email;
    }

    public static class CodeVerificationDTO {
        public String email;
        public String code;
    }

    public static class ResetPasswordDTO {
        public String email;
        public String code;
        public String newPassword;
    }

    public static class LoginVerificationDTO {
        public String email;
        public String code;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserDTO newUserDTO) {
        if (newUserDTO.username == null || newUserDTO.username.trim().isEmpty() ||
            newUserDTO.password == null || newUserDTO.password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username and password cannot be empty.");
        }

        boolean created = userService.registerUser(newUserDTO.username, newUserDTO.password, newUserDTO.email);
        if (created) {
            if (newUserDTO.email != null && !newUserDTO.email.trim().isEmpty()) {
                emailService.sendVerificationCode(newUserDTO.email.trim());
            }
            return ResponseEntity.ok("User account successfully created.");
        } else {
            return ResponseEntity.status(400).body("Username or email already exists.");
        }
    }

    @PostMapping("/login")
public ResponseEntity<Map<String, Object>> login(@RequestBody UserDTO loginDTO) {
    try {
        if (loginDTO.username == null || loginDTO.username.trim().isEmpty() ||
            loginDTO.password == null || loginDTO.password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password cannot be empty."));
        }

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginDTO.username, loginDTO.password)
        );

        User user = userService.findByUsername(loginDTO.username);
        if (user == null) {
            return ResponseEntity.status(400).body(Map.of("error", "User not found."));
        }

        // If user has email, require verification code after password match
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            String code = userService.generateLoginVerificationCode(user.getEmail());
            emailService.sendVerificationEmail(user.getEmail(), code);
            return ResponseEntity.status(202).body(Map.of(
                    "message", "Verification code sent to email. Please verify.",
                    "partialToken", user.getEmail()
            ));
        }

        // No email: proceed with regular login
        String jwt = jwtUtil.generateToken(user.getUsername());
        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("username", user.getUsername());
        response.put("hasBeenPromptedForProfilePic", user.isHasBeenPromptedForProfilePic());

        if (user.getProfilePicture() != null) {
            String imageUrl = "http://localhost:8080/api/users/" + user.getUsername() + "/profile-picture";
            response.put("profilePicUrl", imageUrl);
        } else {
            response.put("profilePicUrl", null);
        }

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password."));
    }
}


    @PostMapping("/verify-login-code")
    public ResponseEntity<Map<String, Object>> verifyLoginCode(@RequestBody LoginVerificationDTO dto) {
        if (dto.email == null || dto.code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and code are required."));
        }

        boolean valid = userService.verifyLoginCode(dto.email.trim(), dto.code.trim());
        if (!valid) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired verification code."));
        }

        User user = userService.findByEmail(dto.email.trim());
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found."));
        }

        String jwt = jwtUtil.generateToken(user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("username", user.getUsername());
        response.put("hasBeenPromptedForProfilePic", user.isHasBeenPromptedForProfilePic());

        if (user.getProfilePicture() != null) {
            String imageUrl = "http://localhost:8080/api/users/" + user.getUsername() + "/profile-picture";
            response.put("profilePicUrl", imageUrl);
        } else {
            response.put("profilePicUrl", null);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<String> sendVerificationCode(@RequestBody EmailVerificationDTO dto) {
        if (dto.email == null || dto.email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }
        emailService.sendVerificationCode(dto.email.trim());
        return ResponseEntity.ok("Verification code sent to email.");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestBody CodeVerificationDTO dto) {
        boolean valid = emailService.verifyCode(dto.email.trim(), dto.code.trim());
        return valid ? ResponseEntity.ok("Email verified.")
                     : ResponseEntity.status(400).body("Invalid or expired verification code.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody EmailVerificationDTO dto) {
        if (dto.email == null || dto.email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }
        if (!userService.existsByEmail(dto.email.trim())) {
            return ResponseEntity.status(404).body("No user found with this email.");
        }
        emailService.sendVerificationCode(dto.email.trim());
        return ResponseEntity.ok("Password reset code sent to email.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordDTO dto) {
        boolean verified = emailService.verifyCode(dto.email.trim(), dto.code.trim());
        if (!verified) {
            return ResponseEntity.status(400).body("Invalid or expired reset code.");
        }
        boolean updated = userService.updatePasswordByEmail(dto.email.trim(), dto.newPassword.trim());
        return updated ? ResponseEntity.ok("Password updated successfully.")
                       : ResponseEntity.status(500).body("Failed to update password.");
    }

    @PostMapping("/{username}/upload-profile-picture")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @PathVariable String username,
            @RequestParam("image") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Image file is required."));
            }

            boolean success = userService.saveProfilePicture(username, file);
            if (success) {
                String imageUrl = "http://localhost:8080/api/users/" + username + "/profile-picture";
                return ResponseEntity.ok(Map.of("profilePicUrl", imageUrl));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "User not found."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error uploading profile picture."));
        }
    }

    @PutMapping("/{username}/mark-prompted")
    public ResponseEntity<?> markPrompted(@PathVariable String username) {
        try {
            boolean success = userService.markUserPrompted(username);
            return success ? ResponseEntity.ok().build() :
                    ResponseEntity.status(404).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating prompt status.");
        }
    }

    @GetMapping("/{username}/profile-picture")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable String username) {
        byte[] imageData = userService.getProfilePicture(username);
        if (imageData == null || imageData.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String contentTypeString = userService.getProfilePictureContentType(username);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        if (contentTypeString != null && !contentTypeString.isEmpty()) {
            try {
                mediaType = MediaType.parseMediaType(contentTypeString);
            } catch (IllegalArgumentException ignored) {}
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
    }
}





















