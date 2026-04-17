package com.example.fairshare.ui.groups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.GroupRepository;
import com.example.fairshare.GroupExpense;
import com.example.fairshare.SettlementCalculator;
import com.example.fairshare.SettlementCalculator.SettlementDetail;
import com.example.fairshare.R;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupSettleUpFragment extends Fragment {

    private GroupRepository groupRepository;
    private String groupId;
    private String currentUserId;
    private SettlementDetailAdapter settlementAdapter;
    private RecyclerView rvSettlements;
    private MaterialTextView tvSettleUpEmpty;
    private View layoutEmptySettlements;
    private Map<String, String> memberNames = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_settle_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        groupRepository = new GroupRepository();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : null;

        // Get group ID from arguments
        Bundle args = getArguments();
        if (args != null) {
            groupId = args.getString("GROUP_ID");
        }

        // Initialize UI elements
        rvSettlements = view.findViewById(R.id.rvSettlements);
        tvSettleUpEmpty = view.findViewById(R.id.tvSettleUpEmpty);
        layoutEmptySettlements = view.findViewById(R.id.layoutEmptySettlements);

        // Setup RecyclerView with settlement adapter
        rvSettlements.setLayoutManager(new LinearLayoutManager(requireContext()));
        settlementAdapter = new SettlementDetailAdapter();
        if (currentUserId != null) {
            settlementAdapter.setCurrentUserId(currentUserId);
        }
        rvSettlements.setAdapter(settlementAdapter);

        // Load expenses and calculate settlements
        if (groupId != null) {
            loadSettlements();
        }
    }

    private void loadSettlements() {
        // Observe expenses for this group
        groupRepository.getGroupExpenses(groupId).observe(getViewLifecycleOwner(), expenses -> {
            if (expenses == null || expenses.isEmpty()) {
                layoutEmptySettlements.setVisibility(View.VISIBLE);
                rvSettlements.setVisibility(View.GONE);
                if (tvSettleUpEmpty != null) {
                    tvSettleUpEmpty.setText("No expenses yet");
                }
            } else {
                layoutEmptySettlements.setVisibility(View.GONE);
                rvSettlements.setVisibility(View.VISIBLE);

                // Load member names first
                loadMemberNames(expenses);

                // Calculate settlements with correct parameter order
                List<SettlementDetail> settlements = SettlementCalculator.calculateSettlements(
                        currentUserId,
                        expenses,
                        memberNames
                );

                // Update adapter
                settlementAdapter.setMemberNames(memberNames);
                settlementAdapter.submitList(settlements);
            }
        });
    }

    private void loadMemberNames(List<GroupExpense> expenses) {
        // Extract all unique UIDs from expenses
        java.util.Set<String> uniqueUids = new java.util.HashSet<>();
        for (GroupExpense expense : expenses) {
            uniqueUids.add(expense.getPayerUid());
            if (expense.getParticipants() != null) {
                uniqueUids.addAll(expense.getParticipants());
            }
        }

        // Fetch display names for each UID
        for (String uid : uniqueUids) {
            if (memberNames.containsKey(uid)) continue;

            if (currentUserId != null && currentUserId.equals(uid)) {
                memberNames.put(uid, "You");
            } else {
                // Pre-populate with fallback
                memberNames.put(uid, "Member " + uid.substring(0, Math.min(4, uid.length())));

                // Fetch actual name from Firestore
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String displayName = doc.getString("displayName");
                                if (displayName != null && !displayName.isEmpty()) {
                                    memberNames.put(uid, displayName);
                                    settlementAdapter.setMemberNames(memberNames);
                                    // Reload settlements with updated names
                                    if (groupId != null) {
                                        groupRepository.getGroupExpenses(groupId).observe(getViewLifecycleOwner(), expenses1 -> {
                                            List<SettlementDetail> settlements = SettlementCalculator.calculateSettlements(
                                                    currentUserId,
                                                    expenses1,
                                                    memberNames
                                            );
                                            settlementAdapter.submitList(settlements);
                                        });
                                    }
                                }
                            }
                        });
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupRepository != null) {
            groupRepository.removeListeners();
        }
    }
}
