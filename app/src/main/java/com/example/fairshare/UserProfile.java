package com.example.fairshare;

/**
 * Represents a user profile stored in the Firestore "users" collection.
 * Document ID is the Firebase Auth UID.
 */
public class UserProfile {

    private String displayName;
    private String displayNameLower; // for case-insensitive search
    private String tagline;          // 4-char alphanumeric (e.g. "A7X2")
    private String email;
    private String phoneNumber;
    private String location;
    private String photoUrl;

    // Required empty constructor for Firestore deserialization
    public UserProfile() {
    }

    public UserProfile(String displayName, String tagline, String email) {
        this.displayName = displayName;
        this.displayNameLower = displayName != null ? displayName.toLowerCase() : null;
        this.tagline = tagline;
        this.email = email;
    }

    // --- Getters & Setters ---

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.displayNameLower = displayName != null ? displayName.toLowerCase() : null;
    }

    public String getDisplayNameLower() {
        return displayNameLower;
    }

    public void setDisplayNameLower(String displayNameLower) {
        this.displayNameLower = displayNameLower;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    /**
     * Returns the formatted identity string: DisplayName#Tagline
     */
    public String getFormattedIdentity() {
        String name = displayName != null ? displayName : "User";
        String tag = tagline != null ? tagline : "0000";
        return name + "#" + tag;
    }
}
