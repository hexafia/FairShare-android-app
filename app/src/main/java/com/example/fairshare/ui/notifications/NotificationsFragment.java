package com.example.fairshare.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
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
        
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                       FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    private void setupRecyclerViews() {
        // Setup nudges RecyclerView
        nudgeAdapter = new NotificationAdapter(requireContext(), new ArrayList<>(), 
            this::onNudgeClick);
        rvNudges.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNudges.setAdapter(nudgeAdapter);

        // Setup payment history RecyclerView
        paymentHistoryAdapter = new NotificationAdapter(requireContext(), new ArrayList<>(), 
            this::onPaymentHistoryClick);
        rvPaymentHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPaymentHistory.setAdapter(paymentHistoryAdapter);
    }

    private void loadNotifications() {
        // Load nudges
        db.collection("notifications")
            .whereEqualTo("recipientUid", currentUserId)
            .whereEqualTo("type", "nudge")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading nudges", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                List<Notification> nudges = new ArrayList<>();
                if (value != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            notification.setId(doc.getId());
                            nudges.add(notification);
                        }
                    }
                }
                
                updateNudgesUI(nudges);
            });

        // Load payment confirmations
        db.collection("notifications")
            .whereEqualTo("recipientUid", currentUserId)
            .whereEqualTo("type", "payment_confirmed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading payment history", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                List<Notification> paymentConfirmations = new ArrayList<>();
                if (value != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            notification.setId(doc.getId());
                            paymentConfirmations.add(notification);
                        }
                    }
                }
                
                updatePaymentHistoryUI(paymentConfirmations);
            });
    }

    private void updateNudgesUI(List<Notification> nudges) {
        if (nudges.isEmpty()) {
            tvNudgesEmpty.setVisibility(View.VISIBLE);
            rvNudges.setVisibility(View.GONE);
        } else {
            tvNudgesEmpty.setVisibility(View.GONE);
            rvNudges.setVisibility(View.VISIBLE);
            nudgeAdapter.updateNotifications(nudges);
        }
    }

    private void updatePaymentHistoryUI(List<Notification> paymentConfirmations) {
        if (paymentConfirmations.isEmpty()) {
            tvPaymentHistoryEmpty.setVisibility(View.VISIBLE);
            rvPaymentHistory.setVisibility(View.GONE);
        } else {
            tvPaymentHistoryEmpty.setVisibility(View.GONE);
            rvPaymentHistory.setVisibility(View.VISIBLE);
            paymentHistoryAdapter.updateNotifications(paymentConfirmations);
        }
    }

    private void onNudgeClick(Notification notification) {
        // Navigate to Group Lobby's Settle Up tab
        Intent intent = new Intent(requireContext(), GroupLobbyActivity.class);
        intent.putExtra("groupId", notification.getGroupId());
        intent.putExtra("groupName", notification.getGroupName());
        intent.putExtra("openSettleUpTab", true);
        startActivity(intent);
        
        // Mark notification as read
        markNotificationAsRead(notification.getId());
    }

    private void onPaymentHistoryClick(Notification notification) {
        // Navigate to Group Lobby's Ledger tab
        Intent intent = new Intent(requireContext(), GroupLobbyActivity.class);
        intent.putExtra("groupId", notification.getGroupId());
        intent.putExtra("groupName", notification.getGroupName());
        startActivity(intent);
        
        // Mark notification as read
        markNotificationAsRead(notification.getId());
    }

    private void markNotificationAsRead(String notificationId) {
        db.collection("notifications")
            .document(notificationId)
            .update("isRead", true);
    }

    @Override
    public void onFastAction() {
        Toast.makeText(requireContext(), "No fast action available here", Toast.LENGTH_SHORT).show();
    }
}
