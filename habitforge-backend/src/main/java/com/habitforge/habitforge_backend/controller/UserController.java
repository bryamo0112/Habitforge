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
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")  
public class UserController {

    private static final Logger LOGGER = Logger.getLogger(UserController.class.getName());

    @Autowired private UserService userService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private EmailService emailService;
    @Autowired private AuthenticationManager authenticationManager;

    public static class UserDTO {
        public String username;
        public String password;
        public String email;
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

    private String extractUsernameFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        return jwtUtil.extractUsername(token);
    }

    // === SIGNUP ===
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserDTO newUserDTO) {
        if (newUserDTO.username == null || newUserDTO.username.trim().isEmpty() ||
            newUserDTO.password == null || newUserDTO.password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username and password cannot be empty.");
        }

        boolean created = userService.registerUser(newUserDTO.username, newUserDTO.password, newUserDTO.email);
        if (!created) {
            return ResponseEntity.status(400).body("Username or email already exists.");
        }

        if (newUserDTO.email != null && !newUserDTO.email.trim().isEmpty()) {
            emailService.sendVerificationCode(newUserDTO.email.trim());
        }

        return ResponseEntity.ok("User account successfully created.");
    }

    // === USERNAME/PASSWORD LOGIN (patched) ===
@PostMapping("/login")
public ResponseEntity<Map<String, Object>> login(@RequestBody UserDTO loginDTO) {
    try {
        // ✅ Block email-only logins here — should use /send-verification-code instead
        boolean isEmailOnlyLogin = 
            (loginDTO.username == null || loginDTO.username.trim().isEmpty()) &&
            (loginDTO.password == null || loginDTO.password.trim().isEmpty());

        if (isEmailOnlyLogin) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email-only login must use /send-verification-code."));
        }

        // ✅ Validate input
        if (loginDTO.username == null || loginDTO.password == null ||
            loginDTO.username.trim().isEmpty() || loginDTO.password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password cannot be empty."));
        }

        // ✅ Authenticate credentials
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginDTO.username, loginDTO.password)
        );

        User user = userService.findByUsername(loginDTO.username);
        if (user == null) {
            return ResponseEntity.status(400).body(Map.of("error", "User not found."));
        }

        // ✅ Require code only if email is present AND not yet verified
        if (user.getEmail() != null && !user.getEmail().isEmpty() && !user.isEmailVerified()) {
            String code = userService.generateLoginVerificationCode(user.getEmail());
            emailService.sendVerificationEmail(user.getEmail(), code);
            return ResponseEntity.status(202).body(Map.of(
                "message", "Verification code sent to email. Please verify.",
                "partialToken", user.getEmail()
            ));
        }

        // ✅ Email verified or not present — proceed to full login
        String usernameForToken = user.getUsername() != null && !user.getUsername().isEmpty()
                ? user.getUsername()
                : user.getEmail();

        String jwt = jwtUtil.generateToken(usernameForToken);
        return buildLoginResponse(user, jwt);

    } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Login failed: " + e.getMessage(), e);
        return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password."));
    }
}



    // === EMAIL-ONLY LOGIN START ===
    @PostMapping("/send-verification-code")
public ResponseEntity<String> sendVerificationCode(@RequestBody EmailVerificationDTO dto) {
    if (dto.email == null || dto.email.trim().isEmpty()) {
        return ResponseEntity.badRequest().body("Email is required.");
    }

    String email = dto.email.trim();

    User existing = userService.findByEmail(email);
    if (existing == null) {
        userService.createUserFromEmailLogin(email);
    }

    // Generate login verification code (saves to loginVerificationCode field)
    String code = userService.generateLoginVerificationCode(email);

    // Send the code via email
    emailService.sendVerificationEmail(email, code);

    return ResponseEntity.ok("Verification code sent to email.");
}



    @PostMapping("/verify-code")
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

        String usernameForToken = user.getUsername() != null && !user.getUsername().isEmpty()
                ? user.getUsername()
                : user.getEmail();

        String jwt = jwtUtil.generateToken(usernameForToken);
        return buildLoginResponse(user, jwt);
    }

    // === SET EMAIL POST LOGIN ===
    @PostMapping("/set-email")
    public ResponseEntity<String> setEmailDirectly(@RequestHeader("Authorization") String authHeader,
                                                   @RequestBody EmailVerificationDTO dto) {
        String username = extractUsernameFromAuthHeader(authHeader);
        if (username == null || dto.email == null || dto.email.trim().isEmpty()) {
            return ResponseEntity.status(403).body("Invalid token or email.");
        }

        boolean updated = userService.updateEmail(username, dto.email.trim());
        return updated ? ResponseEntity.ok("Email set successfully.")
                       : ResponseEntity.badRequest().body("Email already in use or invalid.");
    }

    // === FORGOT/RESET PASSWORD ===

@PostMapping("/forgot-password")
public ResponseEntity<String> forgotPassword(@RequestBody EmailVerificationDTO dto) {
    if (dto.email == null || dto.email.trim().isEmpty()) {
        return ResponseEntity.badRequest().body("Email is required.");
    }

    String email = dto.email.trim();
    if (!userService.existsByEmail(email)) {
        return ResponseEntity.status(404).body("No user found with this email.");
    }

    // Generate a single code for both purposes
    String code = emailService.generateCode();

    // Save the password reset code to the user
    userService.generateAndSendPasswordResetCode(email, code);
    emailService.sendPasswordResetEmail(email, code);      // Main purpose

    return ResponseEntity.ok("Password reset code sent to email.");
}


@PostMapping("/reset-password")
public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordDTO dto) {
    String email = dto.email.trim();
    String code = dto.code.trim();
    String newPassword = dto.newPassword.trim();

    // Verify the reset code using UserService method you added
    if (!userService.verifyResetCode(email, code)) {
        return ResponseEntity.status(400).body("Invalid or expired reset code.");
    }

    // Reset the password (this method will clear the reset code if successful)
    boolean updated = userService.resetPassword(email, code, newPassword);

    return updated
        ? ResponseEntity.ok("Password updated successfully.")
        : ResponseEntity.status(500).body("Failed to update password.");
}


    // === PROFILE PICTURE ===
    @PostMapping("/{username}/upload-profile-picture")
    public ResponseEntity<?> uploadProfilePicture(
            @PathVariable String username,
            @RequestParam("image") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String tokenUsername = extractUsernameFromAuthHeader(authHeader);
        if (tokenUsername == null || !tokenUsername.equals(username)) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized."));
        }

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Image file is required."));
            }

            boolean success = userService.saveProfilePicture(username, file);
            if (success) {
                User updatedUser = userService.findByUsername(username);
                String jwt = extractJwtFromAuthHeader(authHeader);
                return buildLoginResponse(updatedUser, jwt);  // Return the proper DTO
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "User not found."));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error uploading profile picture: " + e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Error uploading profile picture."));
        }
    }

    // Helper method to extract JWT token string from Authorization header
    private String extractJwtFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
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

    // === MARK PROMPTED ===
    @PutMapping("/{username}/mark-prompted")
    public ResponseEntity<?> markPrompted(@PathVariable String username,
                                          @RequestHeader("Authorization") String authHeader) {
        String tokenUsername = extractUsernameFromAuthHeader(authHeader);
        if (tokenUsername == null || !tokenUsername.equals(username)) {
            return ResponseEntity.status(403).body("Unauthorized.");
        }
        try {
            boolean success = userService.markUserPrompted(username);
            return success ? ResponseEntity.ok().build() :
                    ResponseEntity.status(404).body("User not found.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating prompt status: " + e.getMessage(), e);
            return ResponseEntity.status(500).body("Error updating prompt status.");
        }
    }

    // === GET CURRENT USER ===
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String username = extractUsernameFromAuthHeader(authHeader);
        if (username == null) return ResponseEntity.status(403).body("Unauthorized");

        User user = userService.findByUsername(username);
        if (user == null) user = userService.findByEmail(username);
        if (user == null) return ResponseEntity.status(404).body("User not found.");

        ResponseEntity<Map<String, Object>> response = buildLoginResponse(user, authHeader.substring(7));
        LOGGER.info("CurrentUser response: " + response.getBody());
        return response;
    }

    // === SET USERNAME (patched) ===
    @PostMapping("/set-username")
    public ResponseEntity<?> setUsername(@RequestHeader("Authorization") String authHeader,
                                         @RequestBody Map<String, String> body) {
        String currentIdentifier = extractUsernameFromAuthHeader(authHeader);
        if (currentIdentifier == null || !body.containsKey("username") || body.get("username").trim().isEmpty()) {
            return ResponseEntity.status(403).body("Unauthorized or missing new username.");
        }

        String newUsername = body.get("username").trim();
        if (userService.findByUsername(newUsername) != null) {
            return ResponseEntity.badRequest().body("Username already taken.");
        }

        User user = userService.findByUsername(currentIdentifier);
        if (user == null) {
            user = userService.findByEmail(currentIdentifier); // fallback if token was email
            if (user == null) {
                return ResponseEntity.status(404).body("User not found.");
            }
        }

        // Use new updateUsername(User user, String newUsername) method here:
        boolean updated = userService.updateUsername(user, newUsername);
        if (!updated) {
            return ResponseEntity.status(500).body("Failed to update username.");
        }

        String jwt = jwtUtil.generateToken(newUsername);
        return buildLoginResponse(userService.findByUsername(newUsername), jwt);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("Logout successful.");
    }

    // === FINAL LOGIN RESPONSE FORMAT ===
    private ResponseEntity<Map<String, Object>> buildLoginResponse(User user, String jwt) {
    Map<String, Object> response = new HashMap<>();
    response.put("token", jwt);
    response.put("username", user.getUsername());
    response.put("email", user.getEmail());
    response.put("emailVerified", user.isEmailVerified());
    response.put("hasBeenPromptedForProfilePic", user.isHasBeenPromptedForProfilePic());

    boolean hasProfilePicture = false;
    try {
        hasProfilePicture = user.getProfilePicture() != null && user.getProfilePicture().length > 0;
    } catch (Exception e) {
        // handle null or error gracefully
    }

    if (hasProfilePicture) {
        String cacheBuster = "?t=" + System.currentTimeMillis();
        response.put("profilePicUrl", "http://localhost:8080/api/users/" + user.getUsername() + "/profile-picture" + cacheBuster);
    } else {
        response.put("profilePicUrl", null);
    }

    return ResponseEntity.ok(response);
}

}





























