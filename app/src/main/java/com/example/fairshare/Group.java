package com.example.fairshare;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a shared expense group stored in Firebase Realtime Database
 * at path: groups/{groupId}
 */
public class Group {

    private String id;
    private String name;
    private String shareCode;
    private String createdBy;
    private Map<String, Boolean> members;

    // Required empty constructor for Firebase deserialization
    public Group() {
        members = new HashMap<>();
    }

    public Group(String name, String shareCode, String createdBy) {
        this.name = name;
        this.shareCode = shareCode;
        this.createdBy = createdBy;
        this.members = new HashMap<>();
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShareCode() {
        return shareCode;
    }

    public void setShareCode(String shareCode) {
        this.shareCode = shareCode;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }

    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }
}
