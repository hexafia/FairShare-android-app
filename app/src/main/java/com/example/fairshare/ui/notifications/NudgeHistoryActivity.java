package com.example.fairshare.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.Notification;
import com.example.fairshare.R;
import com.example.fairshare.ui.groups.GroupLobbyActivity;
import com.example.fairshare.ui.notifications.NotificationAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NudgeHistoryActivity extends AppCompatActivity {

    private static final String TAG = "NudgeHistoryActivity";
    
    private RecyclerView rvNudges;
    private TextView tvEmpty;
    private NotificationAdapter nudgeAdapter;
    private FirebaseFirestore db;
    private String currentUserId;
    
    // Filter chips
    private Chip chipUnread, chipRead;
    
    // Notification list
    private List<Notification> allNudges = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nudge_history);
        
        initViews();
        setupRecyclerView();
        loadNudges();
        setupFilterChips();
    }

    private void initViews() {
        rvNudges = findViewById(R.id.rvNudges);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        // Filter chips
        chipUnread = findViewById(R.id.chipUnread);
        chipRead = findViewById(R.id.chipRead);
        
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                       FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    private void setupRecyclerView() {
        nudgeAdapter = new NotificationAdapter(this, new ArrayList<>(), this::onNudgeClick);
        rvNudges.setLayoutManager(new LinearLayoutManager(this));
        rvNudges.setAdapter(nudgeAdapter);
    }

    private void setupFilterChips() {
        chipUnread.setChecked(true);
        chipRead.setChecked(false);
        
        chipUnread.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                chipRead.setChecked(false);
            } else if (!chipRead.isChecked()) {
                chipRead.setChecked(true);
            }
            updateNudgesUI(getFilteredNudges());
        });
        
        chipRead.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                chipUnread.setChecked(false);
            } else if (!chipUnread.isChecked()) {
                chipUnread.setChecked(true);
            }
            updateNudgesUI(getFilteredNudges());
        });
    }

    private void loadNudges() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Loading nudges for user: " + currentUserId);
        
        // Load all nudge notifications for current user
        db.collection("notifications")
            .whereEqualTo("recipientUid", currentUserId)
            .whereEqualTo("type", "nudge")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error loading nudges: " + error.getMessage(), error);
                    Toast.makeText(this, "Error loading nudges: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
                
                allNudges.clear();
                
                if (value != null) {
                    Log.d(TAG, "Found " + value.getDocuments().size() + " nudge documents");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            if (isTestNotification(notification)) {
                                continue;
                            }
                            notification.setId(doc.getId());
                            allNudges.add(notification);
                        }
                    }
                } else {
                    Log.d(TAG, "No nudge documents found");
                }
                
                
                Log.d(TAG, "Updating UI - Total Nudges: " + allNudges.size());
                updateNudgesUI(getFilteredNudges());
            });
    }

    private List<Notification> getFilteredNudges() {
        List<Notification> filtered = new ArrayList<>();
        boolean showUnread = chipUnread.isChecked();
        boolean showRead = chipRead.isChecked();
        
        for (Notification nudge : allNudges) {
            if ((showUnread && !nudge.isRead()) || (showRead && nudge.isRead())) {
                filtered.add(nudge);
            }
        }
        
        return filtered;
    }

    private void updateNudgesUI(List<Notification> nudges) {
        if (nudges.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvNudges.setVisibility(View.GONE);
            tvEmpty.setText(chipUnread.isChecked() ? "No unread nudges" : "No read nudges");
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvNudges.setVisibility(View.VISIBLE);
            nudgeAdapter.updateNotifications(nudges);
        }
    }

    private void onNudgeClick(Notification notification) {
        // Mark notification as read first
        markNotificationAsRead(notification.getId());
        
        // Navigate to Group Lobby's Settle Up tab
        Intent intent = new Intent(this, GroupLobbyActivity.class);
        intent.putExtra("GROUP_ID", notification.getGroupId());
        intent.putExtra("GROUP_NAME", notification.getGroupName());
        intent.putExtra("openSettleUpTab", true);
        startActivity(intent);
    }

    private void markNotificationAsRead(String notificationId) {
        db.collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener(aVoid -> {
                // Update local notification list and refresh UI
                updateNotificationReadStatus(notificationId, true);
            });
    }
    
    private void markAllAsRead() {
        for (Notification nudge : allNudges) {
            if (!nudge.isRead()) {
                markNotificationAsRead(nudge.getId());
            }
        }
    }
    
    private void updateNotificationReadStatus(String notificationId, boolean isRead) {
        for (Notification nudge : allNudges) {
            if (notificationId.equals(nudge.getId())) {
                nudge.setRead(isRead);
                break;
            }
        }
        
        // Refresh UI with filtered results
        updateNudgesUI(getFilteredNudges());
    }

    private boolean isTestNotification(Notification notification) {
        String senderUid = notification.getSenderUid();
        if ("test_user_123".equals(senderUid) || "debug_user".equals(senderUid)) {
            return true;
        }

        String message = notification.getMessage();
        return message != null && message.toLowerCase().contains("test notification");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    public boolean onNavigateUp() {
        // Handle back button to return to previous screen
        finish();
        return true;
    }
}
