package com.habitforge.habitforge_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = true) // <-- allow null for email-only users
    private String password;

    @Column(unique = true)
    private String email;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "email_verification_code")
    private String verificationCode;

    @Column(name = "password_reset_code")
    private String resetPasswordCode;

    @Column(name = "login_verification_code")
    private String loginVerificationCode;

    @Column(name = "profile_picture", columnDefinition = "BYTEA")
    private byte[] profilePicture;

    @Column(name = "profile_picture_content_type")
    private String profilePictureContentType;

    @Column(name = "has_been_prompted_for_profile_pic")
    private boolean hasBeenPromptedForProfilePic = false;

    // Constructors
    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String email) {
        this.email = email;
    }

    // Getters & Setters
    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public String getResetPasswordCode() { return resetPasswordCode; }
    public void setResetPasswordCode(String resetPasswordCode) { this.resetPasswordCode = resetPasswordCode; }

    public String getLoginVerificationCode() { return loginVerificationCode; }
    public void setLoginVerificationCode(String loginVerificationCode) { this.loginVerificationCode = loginVerificationCode; }

    public byte[] getProfilePicture() { return profilePicture; }
    public void setProfilePicture(byte[] profilePicture) { this.profilePicture = profilePicture; }

    public String getProfilePictureContentType() { return profilePictureContentType; }
    public void setProfilePictureContentType(String profilePictureContentType) { this.profilePictureContentType = profilePictureContentType; }

    public boolean isHasBeenPromptedForProfilePic() { return hasBeenPromptedForProfilePic; }
    public void setHasBeenPromptedForProfilePic(boolean hasBeenPromptedForProfilePic) {
        this.hasBeenPromptedForProfilePic = hasBeenPromptedForProfilePic;
    }
}














