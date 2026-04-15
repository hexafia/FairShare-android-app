package com.example.fairshare.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.Notification;
import com.example.fairshare.R;
import com.example.fairshare.ui.groups.GroupLobbyActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying all nudge notifications
 * Shows notifications where type == "nudge" for the current user
 */
public class NudgesActivity extends AppCompatActivity {

    private static final String TAG = "NudgesActivity";

    private RecyclerView rvNudges;
    private TextView tvEmpty;
    private NotificationAdapter nudgeAdapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<Notification> allNudges = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nudges);

        initViews();
        setupRecyclerView();
        loadNudges();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        rvNudges = findViewById(R.id.rvNudges);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Nudges");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                       FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    private void setupRecyclerView() {
        nudgeAdapter = new NotificationAdapter(this, new ArrayList<>(), this::onNudgeClick);
        rvNudges.setLayoutManager(new LinearLayoutManager(this));
        rvNudges.setAdapter(nudgeAdapter);
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
                            notification.setId(doc.getId());
                            allNudges.add(notification);
                            
                            // Mark as read when user views this activity
                            markNotificationAsRead(notification.getId());
                        }
                    }
                } else {
                    Log.d(TAG, "No nudge documents found");
                }

                updateUI();
            });
    }

    private void updateUI() {
        if (allNudges.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvNudges.setVisibility(View.GONE);
            tvEmpty.setText("No nudges found");
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvNudges.setVisibility(View.VISIBLE);
            nudgeAdapter.updateNotifications(allNudges);
        }
    }

    private void onNudgeClick(Notification notification) {
        // Navigate to Group Lobby's Settle Up tab
        Intent intent = new Intent(this, GroupLobbyActivity.class);
        intent.putExtra("groupId", notification.getGroupId());
        intent.putExtra("groupName", notification.getGroupName());
        intent.putExtra("openSettleUpTab", true);
        startActivity(intent);
        
        // Mark notification as read (already marked in loadNudges, but ensuring)
        markNotificationAsRead(notification.getId());
    }

    private void markNotificationAsRead(String notificationId) {
        db.collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Marked notification as read: " + notificationId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to mark notification as read: " + notificationId, e);
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when activity resumes
        if (currentUserId != null && !currentUserId.isEmpty()) {
            loadNudges();
        }
    }
}
