package com.habitforge.habitforge_backend.controller;

import com.habitforge.habitforge_backend.model.User;
import com.habitforge.habitforge_backend.security.JwtUtil;
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

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    // DTO class for login and signup requests
    public static class UserDTO {
        public String username;
        public String password;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody(required = true) UserDTO newUserDTO) {
        try {
            if (newUserDTO.username == null || newUserDTO.username.trim().isEmpty() ||
                newUserDTO.password == null || newUserDTO.password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username and password cannot be empty.");
            }

            System.out.println("Signup attempt for user: " + newUserDTO.username);

            boolean created = userService.registerUser(newUserDTO.username, newUserDTO.password);
            if (created) {
                return ResponseEntity.ok("User account successfully created.");
            } else {
                return ResponseEntity.status(400).body("Username already exists.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Could not create user.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody(required = true) UserDTO loginDTO) {
        try {
            if (loginDTO.username == null || loginDTO.username.trim().isEmpty() ||
                loginDTO.password == null || loginDTO.password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password cannot be empty."));
            }

            System.out.println("Login attempt by user: " + loginDTO.username);

            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.username, loginDTO.password)
            );

            User user = userService.findByUsername(loginDTO.username);
            if (user == null) {
                return ResponseEntity.status(400).body(Map.of("error", "User not found."));
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

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password."));
        }
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
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(404).body("User not found.");
            }
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
        MediaType mediaType;

        if (contentTypeString != null && !contentTypeString.isEmpty()) {
            try {
                mediaType = MediaType.parseMediaType(contentTypeString);
            } catch (IllegalArgumentException e) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        } else {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
    }
}



















