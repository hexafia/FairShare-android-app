package com.example.fairshare.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.example.fairshare.GroupExpense;
import com.example.fairshare.Notification;
import com.example.fairshare.R;
import com.example.fairshare.SettlementCalculator;
import com.example.fairshare.ui.groups.GroupLobbyActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment implements com.example.fairshare.FastActionHandler {

    private RecyclerView rvNudges, rvPaymentHistory;
    private TextView tvNudgesEmpty, tvPaymentHistoryEmpty;
    private NotificationAdapter nudgeAdapter, paymentHistoryAdapter;
    private FirebaseFirestore db;
    private String currentUserId;
    
    // Filter chips
    private Chip chipUnread, chipRead, chipUnreadPayments, chipReadPayments;
    private TextView btnViewAllNudges, btnViewAllPayments;
    
    // Notification lists
    private List<Notification> allNudges = new ArrayList<>();
    private List<Notification> allPayments = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerViews();
        loadNotifications();
    }

    private void initViews(View view) {
        rvNudges = view.findViewById(R.id.rvNudges);
        rvPaymentHistory = view.findViewById(R.id.rvPaymentHistory);
        tvNudgesEmpty = view.findViewById(R.id.tvNudgesEmpty);
        tvPaymentHistoryEmpty = view.findViewById(R.id.tvPaymentHistoryEmpty);
        
        // Filter chips
        chipUnread = view.findViewById(R.id.chipUnread);
        chipRead = view.findViewById(R.id.chipRead);
        chipUnreadPayments = view.findViewById(R.id.chipUnreadPayments);
        chipReadPayments = view.findViewById(R.id.chipReadPayments);
        
        // View all buttons
        btnViewAllNudges = view.findViewById(R.id.btnViewAllNudges);
        btnViewAllPayments = view.findViewById(R.id.btnViewAllPayments);
        
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                       FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
                       
        setupFilterChips();
        setupViewAllButtons();
    }

    private void setupRecyclerViews() {
        try {
            // Setup nudges RecyclerView
            if (rvNudges != null) {
                nudgeAdapter = new NotificationAdapter(requireContext(), new ArrayList<>(), 
                    this::onNudgeClick);
                rvNudges.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvNudges.setAdapter(nudgeAdapter);
            } else {
                Log.e("NOTIFICATIONS", "rvNudges is null");
            }

            // Setup payment history RecyclerView
            if (rvPaymentHistory != null) {
                paymentHistoryAdapter = new NotificationAdapter(requireContext(), new ArrayList<>(), 
                    this::onPaymentHistoryClick);
                rvPaymentHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvPaymentHistory.setAdapter(paymentHistoryAdapter);
            } else {
                Log.e("NOTIFICATIONS", "rvPaymentHistory is null");
            }
        } catch (Exception e) {
            Log.e("NOTIFICATIONS", "Error setting up RecyclerViews", e);
        }
    }
    
    private void setupFilterChips() {
        // Set default filter states
        chipUnread.setChecked(true);
        chipUnreadPayments.setChecked(true);
        
        // Nudges filter listeners
        chipUnread.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) chipRead.setChecked(false);
            updateNudgesUI(getFilteredNudges());
        });
        
        chipRead.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) chipUnread.setChecked(false);
            updateNudgesUI(getFilteredNudges());
        });
        
        // Payment history filter listeners
        chipUnreadPayments.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) chipReadPayments.setChecked(false);
            updatePaymentHistoryUI(getFilteredPayments());
        });
        
        chipReadPayments.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) chipUnreadPayments.setChecked(false);
            updatePaymentHistoryUI(getFilteredPayments());
        });
    }
    
    private void setupViewAllButtons() {
        btnViewAllNudges.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NudgeHistoryActivity.class);
            startActivity(intent);
        });
        
        btnViewAllPayments.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettledPaymentsActivity.class);
            startActivity(intent);
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
        
        // Limit to 5 items
        return filtered.size() > 5 ? filtered.subList(0, 5) : filtered;
    }
    
    private List<Notification> getFilteredPayments() {
        List<Notification> filtered = new ArrayList<>();
        boolean showUnread = chipUnreadPayments.isChecked();
        boolean showRead = chipReadPayments.isChecked();
        
        for (Notification payment : allPayments) {
            if ((showUnread && !payment.isRead()) || (showRead && payment.isRead())) {
                filtered.add(payment);
            }
        }
        
        // Limit to 5 items
        return filtered.size() > 5 ? filtered.subList(0, 5) : filtered;
    }

    private void loadNotifications() {
        // Check if currentUserId is valid
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d("NOTIFICATIONS", "Loading notifications for user: " + currentUserId);
        
        // Try ultra-simple query first to test basic access
        db.collection("notifications")
            .limit(10)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e("NOTIFICATIONS", "Basic query failed: " + error.getMessage(), error);
                    Toast.makeText(requireContext(), "Cannot access notifications. Check Firestore rules.", Toast.LENGTH_LONG).show();
                    showFallbackUI();
                    return;
                }
                
                // If basic query works, try the filtered query
                loadFilteredNotifications();
            });
    }
    
    
    private void loadFilteredNotifications() {
        // Load all notifications for current user
        try {
            db.collection("notifications")
                .whereEqualTo("recipientUid", currentUserId)
                .addSnapshotListener((value, error) -> {
                    try {
                        if (error != null) {
                            Log.e("NOTIFICATIONS", "Filtered query failed: " + error.getMessage(), error);
                            
                            // Check if it's a permission error
                            if (error.getMessage() != null && 
                                (error.getMessage().contains("PERMISSION_DENIED") || 
                                 error.getMessage().contains("PRE_CONDITION"))) {
                                Toast.makeText(requireContext(), "Firestore permissions not configured. Please deploy security rules.", Toast.LENGTH_LONG).show();
                                showFallbackUI();
                                return;
                            }
                            
                            Toast.makeText(requireContext(), "Error loading notifications: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            showFallbackUI();
                            return;
                        }
                        
                        List<Notification> allNotifications = new ArrayList<>();
                        
                        if (value != null) {
                            Log.d("NOTIFICATIONS", "Found " + value.getDocuments().size() + " total notification documents");
                            for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                                try {
                                    Notification notification = doc.toObject(Notification.class);
                                    if (notification != null) {
                                        notification.setId(doc.getId());
                                        // Skip test/dummy notifications
                                        if ("test_user_123".equals(notification.getSenderUid())) {
                                            Log.d("NOTIFICATIONS", "Skipping test notification: " + doc.getId());
                                            continue;
                                        }
                                        // Validate notification data
                                        if (notification.getRecipientUid() != null && notification.getType() != null) {
                                            allNotifications.add(notification);
                                        } else {
                                            Log.w("NOTIFICATIONS", "Skipping malformed notification document: " + doc.getId());
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e("NOTIFICATIONS", "Error parsing notification document: " + doc.getId(), e);
                                }
                            }
                        } else {
                            Log.d("NOTIFICATIONS", "No notification documents found");
                        }
                        
                        // Sort notifications by timestamp (newest first)
                        try {
                            allNotifications.sort((n1, n2) -> Long.compare(
                                n2.getTimestamp() != null ? n2.getTimestamp().getTime() : 0,
                                n1.getTimestamp() != null ? n1.getTimestamp().getTime() : 0
                            ));
                        } catch (Exception e) {
                            Log.e("NOTIFICATIONS", "Error sorting notifications", e);
                        }
                        
                        // Separate into nudges and payment confirmations
                        allNudges.clear();
                        allPayments.clear();
                        
                        for (Notification notification : allNotifications) {
                            try {
                                if ("nudge".equals(notification.getType())) {
                                    allNudges.add(notification);
                                    Log.d("NOTIFICATIONS", "Loaded nudge: " + notification.getMessage());
                                } else if ("payment_confirmed".equals(notification.getType())) {
                                    allPayments.add(notification);
                                    Log.d("NOTIFICATIONS", "Loaded payment confirmation: " + notification.getMessage());
                                }
                            } catch (Exception e) {
                                Log.e("NOTIFICATIONS", "Error processing notification: " + notification.getId(), e);
                            }
                        }
                        
                        Log.d("NOTIFICATIONS", "Updating UI - Nudges: " + allNudges.size() + ", Payments: " + allPayments.size());
                        updateNudgesUI(getFilteredNudges());
                        updatePaymentHistoryUI(getFilteredPayments());
                        
                    } catch (Exception e) {
                        Log.e("NOTIFICATIONS", "Error in snapshot listener", e);
                        showFallbackUI();
                    }
                });
        } catch (Exception e) {
            Log.e("NOTIFICATIONS", "Error setting up notification listener", e);
            showFallbackUI();
        }
    }

    private void updateNudgesUI(List<Notification> nudges) {
        try {
            if (nudges == null || nudges.isEmpty()) {
                if (tvNudgesEmpty != null) tvNudgesEmpty.setVisibility(View.VISIBLE);
                if (rvNudges != null) rvNudges.setVisibility(View.GONE);
            } else {
                if (tvNudgesEmpty != null) tvNudgesEmpty.setVisibility(View.GONE);
                if (rvNudges != null) rvNudges.setVisibility(View.VISIBLE);
                if (nudgeAdapter != null) nudgeAdapter.updateNotifications(nudges);
            }
        } catch (Exception e) {
            Log.e("NOTIFICATIONS", "Error updating nudges UI", e);
        }
    }

    private void updatePaymentHistoryUI(List<Notification> paymentConfirmations) {
        try {
            if (paymentConfirmations == null || paymentConfirmations.isEmpty()) {
                if (tvPaymentHistoryEmpty != null) tvPaymentHistoryEmpty.setVisibility(View.VISIBLE);
                if (rvPaymentHistory != null) rvPaymentHistory.setVisibility(View.GONE);
            } else {
                if (tvPaymentHistoryEmpty != null) tvPaymentHistoryEmpty.setVisibility(View.GONE);
                if (rvPaymentHistory != null) rvPaymentHistory.setVisibility(View.VISIBLE);
                if (paymentHistoryAdapter != null) paymentHistoryAdapter.updateNotifications(paymentConfirmations);
            }
        } catch (Exception e) {
            Log.e("NOTIFICATIONS", "Error updating payment history UI", e);
        }
    }

    private void onNudgeClick(Notification notification) {
        // Mark notification as read first
        markNotificationAsRead(notification.getId());
        
        // Navigate to Group Lobby's Settle Up tab using correct extra keys
        Intent intent = new Intent(requireContext(), GroupLobbyActivity.class);
        intent.putExtra("GROUP_ID", notification.getGroupId());
        intent.putExtra("GROUP_NAME", notification.getGroupName());
        intent.putExtra("openSettleUpTab", true);
        startActivity(intent);
    }

    private void onPaymentHistoryClick(Notification notification) {
        // Mark notification as read first
        markNotificationAsRead(notification.getId());
        
        // Navigate to Group Lobby's Ledger tab using correct extra keys
        Intent intent = new Intent(requireContext(), GroupLobbyActivity.class);
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
    
    private void updateNotificationReadStatus(String notificationId, boolean isRead) {
        // Update in allNudges list
        for (Notification nudge : allNudges) {
            if (notificationId.equals(nudge.getId())) {
                nudge.setRead(isRead);
                break;
            }
        }
        
        // Update in allPayments list
        for (Notification payment : allPayments) {
            if (notificationId.equals(payment.getId())) {
                payment.setRead(isRead);
                break;
            }
        }
        
        // Refresh UI with filtered results
        updateNudgesUI(getFilteredNudges());
        updatePaymentHistoryUI(getFilteredPayments());
    }

    @Override
    public void onFastAction() {
        Toast.makeText(requireContext(), "No fast action available here", Toast.LENGTH_SHORT).show();
    }
    
    private void showFallbackUI() {
        // Show empty states with helpful messages
        tvNudgesEmpty.setText("No notifications yet");
        tvNudgesEmpty.setVisibility(View.VISIBLE);
        rvNudges.setVisibility(View.GONE);
        
        tvPaymentHistoryEmpty.setText("No payment history yet");
        tvPaymentHistoryEmpty.setVisibility(View.VISIBLE);
        rvPaymentHistory.setVisibility(View.GONE);
    }
}
