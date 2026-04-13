package com.example.fairshare;

import com.google.firebase.firestore.DocumentId;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a shared expense group stored in Firestore
 * at path: groups/{groupId}
 */
public class Group {

    @DocumentId
    private String id;
    private String name;
    private String shareCode;
    private String createdBy;
    private String status;
    private List<String> members;

    // Required empty constructor for Firebase deserialization
    public Group() {
        members = new ArrayList<>();
    }

    public Group(String name, String shareCode, String createdBy) {
        this.name = name;
        this.shareCode = shareCode;
        this.createdBy = createdBy;
        this.status = "active";
        this.members = new ArrayList<>();
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

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return "active".equals(status);
    }

    public boolean isSettled() {
        return "settled".equals(status);
    }

    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }
}
