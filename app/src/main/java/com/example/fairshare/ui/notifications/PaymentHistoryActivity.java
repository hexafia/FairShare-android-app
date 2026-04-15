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
 * Activity for displaying all payment confirmation notifications
 * Shows notifications where type == "payment_confirmed" for the current user
 */
public class PaymentHistoryActivity extends AppCompatActivity {

    private static final String TAG = "PaymentHistoryActivity";

    private RecyclerView rvPayments;
    private TextView tvEmpty;
    private NotificationAdapter paymentAdapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<Notification> allPayments = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        initViews();
        setupRecyclerView();
        loadPaymentHistory();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        rvPayments = findViewById(R.id.rvPayments);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Payment History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                       FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    private void setupRecyclerView() {
        paymentAdapter = new NotificationAdapter(this, new ArrayList<>(), this::onPaymentClick);
        rvPayments.setLayoutManager(new LinearLayoutManager(this));
        rvPayments.setAdapter(paymentAdapter);
    }

    private void loadPaymentHistory() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Loading payment history for user: " + currentUserId);

        // Load all payment confirmation notifications for current user
        db.collection("notifications")
            .whereEqualTo("recipientUid", currentUserId)
            .whereEqualTo("type", "payment_confirmed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error loading payment history: " + error.getMessage(), error);
                    Toast.makeText(this, "Error loading payment history: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                allPayments.clear();
                
                if (value != null) {
                    Log.d(TAG, "Found " + value.getDocuments().size() + " payment confirmation documents");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            notification.setId(doc.getId());
                            allPayments.add(notification);
                            
                            // Mark as read when user views this activity
                            markNotificationAsRead(notification.getId());
                        }
                    }
                } else {
                    Log.d(TAG, "No payment confirmation documents found");
                }

                updateUI();
            });
    }

    private void updateUI() {
        if (allPayments.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvPayments.setVisibility(View.GONE);
            tvEmpty.setText("No payment history found");
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvPayments.setVisibility(View.VISIBLE);
            paymentAdapter.updateNotifications(allPayments);
        }
    }

    private void onPaymentClick(Notification notification) {
        // Navigate to Group Lobby's Ledger tab
        Intent intent = new Intent(this, GroupLobbyActivity.class);
        intent.putExtra("groupId", notification.getGroupId());
        intent.putExtra("groupName", notification.getGroupName());
        startActivity(intent);
        
        // Mark notification as read (already marked in loadPaymentHistory, but ensuring)
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
            loadPaymentHistory();
        }
    }
}
