package com.example.fairshare.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.Notification;
import com.example.fairshare.R;
import com.example.fairshare.ui.groups.GroupLobbyActivity;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the full history of settled payment confirmations for the current user.
 * Replaces the old PaymentHistoryActivity with the renamed "Settled Payments" concept.
 */
public class SettledPaymentsActivity extends AppCompatActivity {

    private static final String TAG = "SettledPaymentsActivity";

    private RecyclerView rvPayments;
    private TextView tvEmpty;
    private NotificationAdapter paymentAdapter;
    private FirebaseFirestore db;
    private String currentUserId;

    // Filter chips
    private Chip chipUnread, chipRead;

    // Notification list
    private List<Notification> allPayments = new ArrayList<>();

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
        rvPayments = findViewById(R.id.rvSettledPayments);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Filter chips
        chipUnread = findViewById(R.id.chipUnread);
        chipRead = findViewById(R.id.chipRead);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                       FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    private void setupRecyclerView() {
        paymentAdapter = new NotificationAdapter(this, new ArrayList<>(), this::onPaymentClick);
        rvPayments.setLayoutManager(new LinearLayoutManager(this));
        rvPayments.setAdapter(paymentAdapter);
    }

    private void setupFilterChips() {
        // Set default filter states
        chipUnread.setChecked(true);

        // Filter listeners
        chipUnread.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) chipRead.setChecked(false);
            updatePaymentsUI(getFilteredPayments());
        });

        chipRead.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) chipUnread.setChecked(false);
            updatePaymentsUI(getFilteredPayments());
        });
    }

    private void loadSettledPayments() {
        // Check if currentUserId is valid
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Loading settled payments for user: " + currentUserId);

        // Load all payment_confirmed notifications for current user
        db.collection("notifications")
            .whereEqualTo("recipientUid", currentUserId)
            .whereEqualTo("type", "payment_confirmed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error loading settled payments: " + error.getMessage(), error);
                    Toast.makeText(this, "Error loading settled payments", Toast.LENGTH_SHORT).show();
                    return;
                }

                allPayments.clear();

                if (value != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            notification.setId(doc.getId());
                            allPayments.add(notification);
                        }
                    }
                }

                Log.d(TAG, "Loaded " + allPayments.size() + " settled payment confirmations");

                updatePaymentsUI(getFilteredPayments());
            });
    }

    private List<Notification> getFilteredPayments() {
        List<Notification> filtered = new ArrayList<>();
        boolean showUnread = chipUnread.isChecked();
        boolean showRead = chipRead.isChecked();

        for (Notification payment : allPayments) {
            if ((showUnread && !payment.isRead()) || (showRead && payment.isRead())) {
                filtered.add(payment);
            }
        }

        return filtered;
    }

    private void updatePaymentsUI(List<Notification> payments) {
        if (payments.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvPayments.setVisibility(View.GONE);
            tvEmpty.setText(chipUnread.isChecked() ? "No unread settled payments" : "No read settled payments");
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvPayments.setVisibility(View.VISIBLE);
            paymentAdapter.updateNotifications(payments);
        }
    }

    private void onPaymentClick(Notification notification) {
        // Mark notification as read first
        markNotificationAsRead(notification.getId());

        // Navigate to Group Lobby's Ledger tab
        Intent intent = new Intent(this, GroupLobbyActivity.class);
        intent.putExtra("GROUP_ID", notification.getGroupId());
        intent.putExtra("GROUP_NAME", notification.getGroupName());
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

    private void markAllAsRead() {
        for (Notification notification : allPayments) {
            if (!notification.isRead()) {
                markNotificationAsRead(notification.getId());
            }
        }
    }

    private void updateNotificationReadStatus(String notificationId, boolean isRead) {
        // Update in allPayments list
        for (Notification payment : allPayments) {
            if (notificationId.equals(payment.getId())) {
                payment.setRead(isRead);
                break;
            }
        }

        // Refresh UI with filtered results
        updatePaymentsUI(getFilteredPayments());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
