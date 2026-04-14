package com.example.fairshare.ui.groups;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.CurrencyHelper;
import com.example.fairshare.DebtSimplifier;
import com.example.fairshare.GroupExpense;
import com.example.fairshare.Group;
import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.example.fairshare.UserProfile;
import com.example.fairshare.UserRepository;
import com.example.fairshare.SettlementCalculator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class GroupLobbyActivity extends AppCompatActivity {

    private String groupId;
    private String groupName;
    private String shareCode;

    private GroupRepository groupRepository;
    private UserRepository userRepository;
    private GroupExpenseAdapter expenseAdapter;
    private SettlementDetailAdapter settlementAdapter;
    private MembersAdapter membersAdapter;

    private TextView tvGroupTotal, tvMemberCount, tvMyBalance;
    private View layoutLedger, layoutSettleUp, layoutMembers;
    private TextView tvLedgerEmpty, tvSettleEmpty, tvMembersEmpty;
    private RecyclerView rvLedger, rvDebts, rvMembers;
    private MaterialButton btnMarkAsAccomplished;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddExpense;
    private Group currentGroup;

    // Maps UID → display name for the Settle Up tab
    private final Map<String, String> memberNames = new HashMap<>();
    
    // Track selected participants for expense splitting
    private final Map<String, Boolean> selectedParticipants = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_lobby);

        groupId = getIntent().getStringExtra("GROUP_ID");
        groupName = getIntent().getStringExtra("GROUP_NAME");
        shareCode = getIntent().getStringExtra("SHARE_CODE");

        // Bind header views
        TextView tvGroupName = findViewById(R.id.tvGroupName);
        TextView tvShareCode = findViewById(R.id.tvShareCode);
        tvGroupTotal = findViewById(R.id.tvGroupTotal);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvMyBalance = findViewById(R.id.tvMyBalance);

        tvGroupName.setText(groupName);
        tvShareCode.setText(shareCode);

        // Copy code functionality
        View layoutShareCode = findViewById(R.id.layoutShareCode);
        layoutShareCode.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                    getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Group Code", shareCode);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Code copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup tabs
        layoutLedger = findViewById(R.id.layoutLedger);
        layoutSettleUp = findViewById(R.id.layoutSettleUp);
        layoutMembers = findViewById(R.id.layoutMembers);
        tvLedgerEmpty = findViewById(R.id.tvLedgerEmpty);
        tvSettleEmpty = findViewById(R.id.tvSettleEmpty);
        tvMembersEmpty = findViewById(R.id.tvMembersEmpty);
        rvLedger = findViewById(R.id.rvLedger);
        rvDebts = findViewById(R.id.rvDebts);
        rvMembers = findViewById(R.id.rvMembers);
        btnMarkAsAccomplished = findViewById(R.id.btnMarkAsAccomplished);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Hide all layouts first
                layoutLedger.setVisibility(View.GONE);
                layoutSettleUp.setVisibility(View.GONE);
                layoutMembers.setVisibility(View.GONE);
                
                if (tab.getPosition() == 0) {
                    layoutLedger.setVisibility(View.VISIBLE);
                } else if (tab.getPosition() == 1) {
                    layoutSettleUp.setVisibility(View.VISIBLE);
                } else if (tab.getPosition() == 2) {
                    layoutMembers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Setup RecyclerViews
        expenseAdapter = new GroupExpenseAdapter();
        rvLedger.setLayoutManager(new LinearLayoutManager(this));
        rvLedger.setAdapter(expenseAdapter);

        // Setup tabs
        layoutLedger = findViewById(R.id.layoutLedger);
        layoutSettleUp = findViewById(R.id.layoutSettleUp);
        layoutMembers = findViewById(R.id.layoutMembers);
        tvLedgerEmpty = findViewById(R.id.tvLedgerEmpty);
        tvSettleEmpty = findViewById(R.id.tvSettleEmpty);
        tvMembersEmpty = findViewById(R.id.tvMembersEmpty);
        rvLedger = findViewById(R.id.rvLedger);
        rvDebts = findViewById(R.id.rvDebts);
        rvMembers = findViewById(R.id.rvMembers);
        btnMarkAsAccomplished = findViewById(R.id.btnMarkAsAccomplished);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Hide all layouts first
                layoutLedger.setVisibility(View.GONE);
                layoutSettleUp.setVisibility(View.GONE);
                layoutMembers.setVisibility(View.GONE);
                
                if (tab.getPosition() == 0) {
                    layoutLedger.setVisibility(View.VISIBLE);
                } else if (tab.getPosition() == 1) {
                    layoutSettleUp.setVisibility(View.VISIBLE);
                } else if (tab.getPosition() == 2) {
                    layoutMembers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Initialize FAB
        fabAddExpense = findViewById(R.id.fabAddExpense);
        fabAddExpense.setOnClickListener(v -> {
            Log.d("FAB_DEBUG", "FAB clicked!");
            try {
                Log.d("TEST_DEBUG", "About to call showAddExpenseDialog");
                showAddExpenseDialog();
                Log.d("TEST_DEBUG", "Returned from showAddExpenseDialog");
            } catch (Exception e) {
                Log.e("FAB_ERROR", "Error in showAddExpenseDialog: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Initially set FAB as visible for testing
        if (fabAddExpense != null) {
            fabAddExpense.setVisibility(View.VISIBLE);
            Log.d("FAB_DEBUG", "FAB made visible in onCreate");
        }

        // Repositories
        groupRepository = new GroupRepository();
        userRepository = new UserRepository();

        // Load group data
        loadGroupData();

        // Load member names for display
        loadMemberNames();

        // Observe group expenses
        groupRepository.getGroupExpenses(groupId).observe(this, expenses -> {
            expenseAdapter.submitList(expenses);

            if (expenses == null || expenses.isEmpty()) {
                tvLedgerEmpty.setVisibility(View.VISIBLE);
                rvLedger.setVisibility(View.GONE);
                tvGroupTotal.setText("₱0");
                tvMyBalance.setText("₱0");
                tvSettleEmpty.setVisibility(View.VISIBLE);
                rvDebts.setVisibility(View.GONE);
            } else {
                tvLedgerEmpty.setVisibility(View.GONE);
                rvLedger.setVisibility(View.VISIBLE);
                updateStats(expenses);
                updateDebts(expenses);
            }
        });
    }

    private void loadMemberNames() {
        groupRepository.getGroupMembers(groupId, memberUids -> {
            tvMemberCount.setText(String.valueOf(memberUids.size()));

            // For each member UID, look up their display name from the users Firestore collection
            for (String uid : memberUids) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("displayName");
                                if (name != null) {
                                    memberNames.put(uid, name);
                                    // Refresh settlement display with new names
                                    settlementAdapter.setMemberNames(memberNames);
                                    settlementAdapter.notifyDataSetChanged();
                                    // Recalculate settlements with updated member names
                                    if (expenseAdapter.getCurrentList() != null) {
                                        updateDebts(expenseAdapter.getCurrentList());
                                    }
                                    // Also populate members tab
                                    populateMembersTab();
                                }
                            }
                        });
            }
        });
    }

    private void populateMembersTab() {
        if (memberNames.isEmpty()) {
            tvMembersEmpty.setVisibility(View.VISIBLE);
            rvMembers.setVisibility(View.GONE);
        } else {
            tvMembersEmpty.setVisibility(View.GONE);
            rvMembers.setVisibility(View.VISIBLE);

            // Update members adapter with current group data
            if (membersAdapter != null && currentGroup != null) {
                membersAdapter = new MembersAdapter(memberNames, currentGroup.getCreatedBy());
                rvMembers.setAdapter(membersAdapter);

                // Set up member click listener
                membersAdapter.setOnMemberClickListener(this::showMemberProfileDialog);

                java.util.List<String> memberList = new java.util.ArrayList<>(memberNames.keySet());
                membersAdapter.updateMembers(memberList);
            }
        }

    private void showMemberProfileDialog(String memberUid) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_member_profile, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(GroupLobbyActivity.this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Get dialog views
        TextInputEditText etDisplayName = dialogView.findViewById(R.id.etDisplayName);
        TextInputEditText etTagline = dialogView.findViewById(R.id.etTagline);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

        // Fetch member data from Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(memberUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String displayName = doc.getString("displayName");
                        String tagline = doc.getString("tagline");

                        // Set the data in the dialog
                        if (displayName != null) {
                            etDisplayName.setText(displayName);
                        }
                        if (tagline != null) {
                            etTagline.setText(tagline);
                        } else {
                            etTagline.setText("No tagline set");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    etDisplayName.setText("Error loading profile");
                    etTagline.setText("Please try again");
                });

        // Set up close button
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Set dialog size
        if (dialog.getWindow() != null) {
            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void updateStats(List<GroupExpense> expenses) {
        double total = 0;
        Map<String, Double> netBalances = new HashMap<>();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String myUid = currentUser != null ? currentUser.getUid() : "";

        for (GroupExpense e : expenses) {
            total += e.getAmount();

            // Calculate net balance for each participant
            int participantCount = e.getParticipantCount();
            if (participantCount == 0) continue;
            double share = e.getAmount() / participantCount;

            // Payer gains
            netBalances.put(e.getPayerUid(),
                    netBalances.getOrDefault(e.getPayerUid(), 0.0) + e.getAmount());

            // Each participant owes their share
            for (String uid : e.getParticipants()) {
                netBalances.put(uid, netBalances.getOrDefault(uid, 0.0) - share);
            }
        }

        tvGroupTotal.setText(CurrencyHelper.formatWholeNumber(total));

        // My balance
        double myBalance = netBalances.getOrDefault(myUid, 0.0);
        tvMyBalance.setText(CurrencyHelper.formatBalance(myBalance));
    }

    private void updateDebts(List<GroupExpense> expenses) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        List<SettlementCalculator.SettlementDetail> settlements =
                SettlementCalculator.calculateSettlements(currentUser.getUid(), expenses, memberNames);

        settlementAdapter.setMemberNames(memberNames);
        settlementAdapter.submitList(settlements);

        if (settlements.isEmpty()) {
            tvSettleEmpty.setVisibility(View.VISIBLE);
            rvDebts.setVisibility(View.GONE);
        } else {
            tvSettleEmpty.setVisibility(View.GONE);
            rvDebts.setVisibility(View.VISIBLE);
        }
    }
