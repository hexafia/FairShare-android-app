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

public class SettledPaymentsActivity extends AppCompatActivity {

    private static final String TAG = "SettledPaymentsActivity";
    
    private RecyclerView rvSettledPayments;
    private TextView tvEmpty;
    private NotificationAdapter settledPaymentAdapter;
    private FirebaseFirestore db;
    private String currentUserId;
    
    // Filter chips
    private Chip chipUnread, chipRead;
    
    // Notification list
    private List<Notification> allSettledPayments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settled_payments);
        
        initViews();
        setupRecyclerView();
        setupFilterChips();
        loadSettledPayments();
    }
    
    private void initViews() {
        rvSettledPayments = findViewById(R.id.rvSettledPayments);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        // Filter chips
        chipUnread = findViewById(R.id.chipUnread);
        chipRead = findViewById(R.id.chipRead);
        
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                       FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    private void setupRecyclerView() {
        settledPaymentAdapter = new NotificationAdapter(this, new ArrayList<>(), this::onSettledPaymentClick);
        rvSettledPayments.setLayoutManager(new LinearLayoutManager(this));
        rvSettledPayments.setAdapter(settledPaymentAdapter);
    }
    
    private void setupFilterChips() {
        // Set default filter states
        chipUnread.setChecked(true);
        
        // Filter listeners
        chipUnread.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) chipRead.setChecked(false);
            updateSettledPaymentsUI(getFilteredSettledPayments());
        });
        
        chipRead.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) chipUnread.setChecked(false);
            updateSettledPaymentsUI(getFilteredSettledPayments());
        });
    }
    
    private void loadSettledPayments() {
        // Check if currentUserId is valid
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Loading settled payments for user: " + currentUserId);
        
        // Load all settled_payment notifications for current user
        db.collection("notifications")
            .whereEqualTo("recipientUid", currentUserId)
            .whereEqualTo("type", "settled_payment")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error loading settled payments: " + error.getMessage(), error);
                    Toast.makeText(this, "Error loading settled payments", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                allSettledPayments.clear();
                
                if (value != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            Notification notification = doc.toObject(Notification.class);
                            if (notification != null) {
                                notification.setId(doc.getId());
                                // Validate notification data
                                if (notification.getRecipientUid() != null && notification.getType() != null) {
                                    allSettledPayments.add(notification);
                                } else {
                                    Log.w(TAG, "Skipping malformed notification document: " + doc.getId());
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification document: " + doc.getId(), e);
                        }
                    }
                }
                
                Log.d(TAG, "Loaded " + allSettledPayments.size() + " settled payment notifications");
                updateSettledPaymentsUI(getFilteredSettledPayments());
            });
    }
    
    private List<Notification> getFilteredSettledPayments() {
        List<Notification> filtered = new ArrayList<>();
        boolean showUnread = chipUnread.isChecked();
        boolean showRead = chipRead.isChecked();
        
        for (Notification settledPayment : allSettledPayments) {
            if ((showUnread && !settledPayment.isRead()) || (showRead && settledPayment.isRead())) {
                filtered.add(settledPayment);
            }
        }
        
        return filtered;
    }

    private void updateSettledPaymentsUI(List<Notification> settledPayments) {
        try {
            if (settledPayments == null || settledPayments.isEmpty()) {
                if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                if (rvSettledPayments != null) rvSettledPayments.setVisibility(View.GONE);
            } else {
                if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                if (rvSettledPayments != null) rvSettledPayments.setVisibility(View.VISIBLE);
                if (settledPaymentAdapter != null) settledPaymentAdapter.updateNotifications(settledPayments);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating settled payments UI", e);
        }
    }

    private void onSettledPaymentClick(Notification notification) {
        // Mark notification as read first
        markNotificationAsRead(notification.getId());
        
        // Navigate to Group Lobby's Ledger tab
        Intent intent = new Intent(this, GroupLobbyActivity.class);
        intent.putExtra("groupId", notification.getGroupId());
        intent.putExtra("groupName", notification.getGroupName());
        startActivity(intent);
    }

    private void markNotificationAsRead(String notificationId) {
        db.collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener(aVoid -> {
                // Update local notification lists and refresh UI
                updateNotificationReadStatus(notificationId, true);
            });
    }
    
    private void updateNotificationReadStatus(String notificationId, boolean isRead) {
        // Update in allSettledPayments list
        for (Notification settledPayment : allSettledPayments) {
            if (notificationId.equals(settledPayment.getId())) {
                settledPayment.setRead(isRead);
                break;
            }
        }
        
        // Refresh UI with filtered results
        updateSettledPaymentsUI(getFilteredSettledPayments());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Mark all notifications as read when viewing this page
        markAllNotificationsAsRead();
    }
    
    private void markAllNotificationsAsRead() {
        for (Notification notification : allSettledPayments) {
            if (!notification.isRead()) {
                markNotificationAsRead(notification.getId());
            }
        }
    }
    
    @Override
    public boolean onNavigateUp() {
        // Handle back button to return to previous screen
        finish();
        return true;
    }
}
