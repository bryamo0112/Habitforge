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

    @Column(nullable = false)
    private String password;

    @Column(name = "profile_picture", columnDefinition = "BYTEA")
    private byte[] profilePicture;

    @Column(name = "profile_picture_content_type")
    private String profilePictureContentType;

    @Column(name = "has_been_prompted_for_profile_pic")
    private boolean hasBeenPromptedForProfilePic = false;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }
    public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getProfilePictureContentType() {
        return profilePictureContentType;
    }
    public void setProfilePictureContentType(String contentType) {
        this.profilePictureContentType = contentType;
    }

    public boolean isHasBeenPromptedForProfilePic() {
        return hasBeenPromptedForProfilePic;
    }
    public void setHasBeenPromptedForProfilePic(boolean prompted) {
        this.hasBeenPromptedForProfilePic = prompted;
    }
}








