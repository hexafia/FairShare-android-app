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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.fairshare.OcrHelper;
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
    private SettlementDetailAdapter toPayAdapter; // "To Pay" section adapter
    private SettlementDetailAdapter paidAdapter; // "Paid" section adapter
    private MembersAdapter membersAdapter;

    private TextView tvGroupTotal, tvMemberCount, tvMyBalance;
    private View layoutLedger, layoutSettleUp, layoutMembers;
    private TextView tvLedgerEmpty, tvSettleEmpty, tvMembersEmpty;
    private RecyclerView rvLedger, rvDebts, rvMembers;
    private RecyclerView rvToPay, rvPaid; // Two-section layout
    private View layoutToPay, layoutPaid; // Section containers
    private MaterialButton btnMarkAsAccomplished;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddExpense;
    private Group currentGroup;

    // Maps UID ΓåÆ display name for the Settle Up tab
    private final Map<String, String> memberNames = new HashMap<>();
    
    // Track selected participants for expense splitting
    private final Map<String, Boolean> selectedParticipants = new HashMap<>();
    private boolean isEqualSplit = true;
    
    // Store current expenses list for tab refresh
    private List<GroupExpense> currentExpensesList = new ArrayList<>();

    // OCR and Camera
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri currentPhotoUri;
    private TextInputEditText activeAmountEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_lobby);

        // Register camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && currentPhotoUri != null && activeAmountEditText != null) {
                    OcrHelper.extractAmountFromReceipt(this, currentPhotoUri, new OcrHelper.OcrCallback() {
                        @Override
                        public void onSuccess(String amount) {
                            if (amount != null && !amount.isEmpty()) {
                                activeAmountEditText.setText(amount);
                                Toast.makeText(GroupLobbyActivity.this, "Scan successful", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(GroupLobbyActivity.this, "Could not detect total amount", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(GroupLobbyActivity.this, "Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

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
        
        // Two-section layout components
        layoutToPay = findViewById(R.id.layoutToPay);
        layoutPaid = findViewById(R.id.layoutPaid);
        rvToPay = findViewById(R.id.rvToPay);
        rvPaid = findViewById(R.id.rvPaid);
        
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
                    // Force Refresh on Tab Switch - Explicitly call updateDebts for Settle Up tab
                    Log.d("REALTIME_CHECK", "Settle Up tab selected, forcing refresh");
                    if (!currentExpensesList.isEmpty()) {
                        updateDebts(currentExpensesList);
                    }
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

        // Setup two-section adapters
        toPayAdapter = new SettlementDetailAdapter();
        paidAdapter = new SettlementDetailAdapter();
        
        // Settle click listener for "To Pay" section
        toPayAdapter.setOnSettleClickListener(settlement -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Confirm Settlement")
                .setMessage("Mark this debt as settled?")
                .setPositiveButton("Yes", (d, which) -> {
                    groupRepository.markSettled(settlement.expenseId, settlement.debtorUid,
                        new GroupRepository.OnCompleteCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(GroupLobbyActivity.this, message, Toast.LENGTH_SHORT).show();
                                // Force immediate adapter refresh to show visual changes
                                toPayAdapter.notifyDataSetChanged();
                                paidAdapter.notifyDataSetChanged();
                                updateDebts();
                            }
                            @Override
                            public void onError(String error) {
                                Toast.makeText(GroupLobbyActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
        
        // Set up RecyclerViews for two sections
        rvToPay.setLayoutManager(new LinearLayoutManager(this));
        rvToPay.setAdapter(toPayAdapter);
        
        rvPaid.setLayoutManager(new LinearLayoutManager(this));
        rvPaid.setAdapter(paidAdapter);
        
        // Legacy adapter (kept for compatibility)
        settlementAdapter = new SettlementDetailAdapter();
        rvDebts.setLayoutManager(new LinearLayoutManager(this));
        rvDebts.setAdapter(settlementAdapter);

        // Setup Members RecyclerView
        membersAdapter = new MembersAdapter(memberNames, null); // Will update with group data
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(membersAdapter);
        membersAdapter.setOnMemberClickListener(this::showMemberProfileDialog);

        // Mark as Accomplished button
        btnMarkAsAccomplished.setOnClickListener(v -> markGroupAsAccomplished());

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
        
        // Global Observer Setup - MOVED TO END OF onCreate FOR ACTIVITY LIFECYCLE
        // This ensures real-time sync even when Settle Up tab isn't visible
        setupExpenseObserver();
    }
    
    private void setupExpenseObserver() {
        // Global Observer Setup - Using Activity lifecycle owner for real-time sync
        groupRepository.getGroupExpenses(groupId).observe(this, expenses -> {
            Log.d("REALTIME_CHECK", "New expenses received, count: " + (expenses != null ? expenses.size() : 0));
            
            // Store current expenses for tab refresh
            currentExpensesList = expenses != null ? new ArrayList<>(expenses) : new ArrayList<>();
            
            // Update expense adapter first
            expenseAdapter.submitList(expenses);

            // Always call updateDebts to ensure real-time sync for Settle Up tab
            if (expenses == null || expenses.isEmpty()) {
                Log.d("SETTLE_UP_SYNC", "No expenses, showing empty state");
                tvLedgerEmpty.setVisibility(View.VISIBLE);
                rvLedger.setVisibility(View.GONE);
                tvGroupTotal.setText("â¥0");
                tvMyBalance.setText("â¥0");
                tvSettleEmpty.setVisibility(View.VISIBLE);
                rvDebts.setVisibility(View.GONE);
                
                // Clear settlement adapter when no expenses
                settlementAdapter.submitList(new ArrayList<>());
            } else {
                Log.d("SETTLE_UP_SYNC", "Updating stats and debts for " + expenses.size() + " expenses");
                tvLedgerEmpty.setVisibility(View.GONE);
                rvLedger.setVisibility(View.VISIBLE);
                updateStats(expenses);
                updateDebts(expenses); // Explicit call to trigger aggregation
            }
        });
    }

    private void loadMemberNames(java.util.List<String> memberUids) {
        if (memberUids == null) return;
        tvMemberCount.setText(String.valueOf(memberUids.size()));

        // For each member UID, look up their display name from the users Firestore collection
        for (String uid : memberUids) {
            if (memberNames.containsKey(uid)) continue; // Avoid redundant queries

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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String myUid = currentUser != null ? currentUser.getUid() : "";
        double myBalance = 0;

        for (GroupExpense e : expenses) {
            total += e.getAmount();
            Map<String, Double> splits = e.getSplitAmounts();

            if (splits != null && !splits.isEmpty()) {
                // Use explicit splitAmounts for accurate calculation
                // Payer paid the full amount, so they are owed (amount - their own share)
                if (myUid.equals(e.getPayerUid())) {
                    // I paid, so I'm owed everything except my own share
                    double myShare = splits.containsKey(myUid) ? splits.get(myUid) : 0;
                    double owedToMe = e.getAmount() - myShare;
                    // Subtract already-settled amounts
                    for (Map.Entry<String, Double> split : splits.entrySet()) {
                        if (!split.getKey().equals(myUid) && e.isSettledFor(split.getKey())) {
                            owedToMe -= split.getValue();
                        }
                    }
                    myBalance += owedToMe;
                } else if (splits.containsKey(myUid)) {
                    // I owe my share to the payer (unless settled)
                    if (!e.isSettledFor(myUid)) {
                        myBalance -= splits.get(myUid);
                    }
                }
            } else {
                // Fallback: equal split based on participant count
                int participantCount = e.getParticipantCount();
                if (participantCount == 0) continue;
                double share = e.getAmount() / participantCount;

                if (myUid.equals(e.getPayerUid())) {
                    myBalance += (e.getAmount() - share);
                } else if (e.getParticipants() != null && e.getParticipants().contains(myUid)) {
                    myBalance -= share;
                }
            }
        }

        tvGroupTotal.setText(CurrencyHelper.format(total));
        tvMyBalance.setText(CurrencyHelper.formatBalance(myBalance));
    }

    private void updateDebts(List<GroupExpense> expenses) {
        Log.d("REALTIME_CHECK", "Recalculating debts for Settle Up tab");
        Log.d("SETTLE_UP_SYNC", "updateDebts called with " + (expenses != null ? expenses.size() : 0) + " expenses");
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Calculate itemized settlements for all expenses
        List<SettlementCalculator.SettlementDetail> allSettlements = 
                SettlementCalculator.calculateSettlements(currentUser.getUid(), expenses, memberNames);
        
        Log.d("SETTLE_UP_SYNC", "Calculated " + allSettlements.size() + " settlement transactions");
        
        // Separate into "To Pay" (unsettled) and "Paid" (settled) sections
        List<SettlementCalculator.SettlementDetail> toPaySettlements = new ArrayList<>();
        List<SettlementCalculator.SettlementDetail> paidSettlements = new ArrayList<>();
        
        for (SettlementCalculator.SettlementDetail settlement : allSettlements) {
            if (settlement.settled) {
                paidSettlements.add(settlement);
            } else {
                toPaySettlements.add(settlement);
            }
        }
        
        Log.d("SETTLE_UP_SYNC", "To Pay: " + toPaySettlements.size() + ", Paid: " + paidSettlements.size());
        
        // Ensure UI thread safety for adapter updates
        runOnUiThread(() -> {
            // Set member names for both adapters
            toPayAdapter.setMemberNames(memberNames);
            paidAdapter.setMemberNames(memberNames);
            
            // Update adapters with respective data
            toPayAdapter.submitList(toPaySettlements);
            paidAdapter.submitList(paidSettlements);
            
            // Manual notify for complex aggregation
            toPayAdapter.notifyDataSetChanged();
            paidAdapter.notifyDataSetChanged();

            // Show appropriate UI state
            if (allSettlements.isEmpty()) {
                // No settlements at all
                Log.d("SETTLE_UP_SYNC", "No outstanding debts, showing empty state");
                tvSettleEmpty.setVisibility(View.VISIBLE);
                layoutToPay.setVisibility(View.GONE);
                layoutPaid.setVisibility(View.GONE);
            } else {
                Log.d("SETTLE_UP_SYNC", "Showing settlements");
                tvSettleEmpty.setVisibility(View.GONE);
                
                // Show "To Pay" section if there are unsettled items
                if (toPaySettlements.isEmpty()) {
                    layoutToPay.setVisibility(View.GONE);
                } else {
                    layoutToPay.setVisibility(View.VISIBLE);
                }
                
                // Show "Paid" section if there are settled items
                if (paidSettlements.isEmpty()) {
                    layoutPaid.setVisibility(View.GONE);
                } else {
                    layoutPaid.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showAddExpenseDialog() {
        try {
            Log.d("FAB_DEBUG", "Starting inflation");
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_add_group_expense, null);
            Log.d("FAB_DEBUG", "Inflation successful");
            
            AlertDialog.Builder builder = new AlertDialog.Builder(GroupLobbyActivity.this);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();
            Log.d("FAB_DEBUG", "Dialog created, about to show");
            dialog.show();
            Log.d("FAB_DEBUG", "Dialog show() called");
            
            // Declare views
            TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
            TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
            TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);
            TextView tvParticipatedHeader = dialogView.findViewById(R.id.tvParticipatedHeader);
            MaterialButton btnAddExpense = dialogView.findViewById(R.id.btnAddExpense);
            ImageView btnClose = dialogView.findViewById(R.id.btnClose);
            
            // Spinner components
            Spinner spinnerSelectGroup = dialogView.findViewById(R.id.spinnerSelectGroup);
            Spinner spinnerWhoPaid = dialogView.findViewById(R.id.spinnerWhoPaid);
            Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
            LinearLayout containerParticipants = dialogView.findViewById(R.id.containerParticipants);
            
            // Track selected group ID for expense creation
            final String[] selectedGroupId = {groupId}; // Default to current group
            
            // Split method buttons
            Button btnEqualSplit = dialogView.findViewById(R.id.btnEqualSplit);
            Button btnUnequalSplit = dialogView.findViewById(R.id.btnUnequalSplit);
            
            // 0. SPINNER SELECT GROUP INITIALIZATION
            groupRepository.getGroups().observe(this, groups -> {
                if (groups != null) {
                    java.util.ArrayList<String> activeGroupNames = new java.util.ArrayList<>();
                    java.util.ArrayList<String> activeGroupIds = new java.util.ArrayList<>();
                    for (Group group : groups) {
                        if (group.isActive()) {
                            activeGroupNames.add(group.getName());
                            activeGroupIds.add(group.getId());
                        }
                    }
                    ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(GroupLobbyActivity.this,
                        android.R.layout.simple_spinner_item, activeGroupNames);
                    groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSelectGroup.setAdapter(groupAdapter);
                    spinnerSelectGroup.setClickable(true);
                    spinnerSelectGroup.setEnabled(true);
                    
                    int currentPosition = activeGroupIds.indexOf(groupId);
                    if (currentPosition >= 0) {
                        spinnerSelectGroup.setSelection(currentPosition);
                    }
                    
                    // We don't implement full re-rendering for other groups yet since our 
                    // authoritative UI relies heavily on the active lobby's stream. We just 
                    // track the ID so the expense saves to the chosen group.
                    spinnerSelectGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedGroupId[0] = activeGroupIds.get(position);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
            });
            spinnerSelectGroup.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
            
            // 1. SPINNER INITIALIZATION
            // Build the full ordered member list from currentGroup (authoritative source)
            List<String> allMemberUids = (currentGroup != null && currentGroup.getMembers() != null)
                    ? currentGroup.getMembers() : new ArrayList<>(memberNames.keySet());

            // Build parallel lists for the spinner: uid -> display name (fallback to short uid)
            final List<String> whoPaidUids = new ArrayList<>(allMemberUids);
            ArrayList<String> memberNamesList = new ArrayList<>();
            for (String uid : whoPaidUids) {
                memberNamesList.add(memberNames.containsKey(uid) ? memberNames.get(uid) : uid.substring(0, Math.min(6, uid.length())));
            }

            ArrayAdapter<String> whoPaidAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, memberNamesList);
            whoPaidAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerWhoPaid.setAdapter(whoPaidAdapter);
            spinnerWhoPaid.setClickable(true);
            spinnerWhoPaid.setEnabled(true);
            
            // Category Spinner - populate with predefined categories
            String[] categories = {"Food", "Transport", "Bills", "Others"};
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(categoryAdapter);
            spinnerCategory.setClickable(true);
            spinnerCategory.setEnabled(true);
            
            // 2. TOUCH EVENT FIX FOR NESTEDSCROLLVIEW
            spinnerWhoPaid.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
            
            spinnerCategory.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
            
            // 3. PARTICIPANT CHECKLIST (Dynamic Inflation)
            selectedParticipants.clear();
            updateParticipantChecklist(containerParticipants, tvParticipatedHeader, true, allMemberUids);
            
            // 4. EQUAL/UNEQUAL SPLIT TOGGLE
            isEqualSplit = true;
            btnEqualSplit.setOnClickListener(v -> {
                isEqualSplit = true;
                btnEqualSplit.setBackgroundColor(Color.parseColor("#38BDB0"));
                btnEqualSplit.setTextColor(Color.WHITE);
                btnUnequalSplit.setBackgroundColor(Color.parseColor("#EFFFFD"));
                btnUnequalSplit.setTextColor(Color.parseColor("#2D3142"));
                updateParticipantChecklist(containerParticipants, tvParticipatedHeader, true, allMemberUids);
            });
            
            btnUnequalSplit.setOnClickListener(v -> {
                isEqualSplit = false;
                btnUnequalSplit.setBackgroundColor(Color.parseColor("#38BDB0"));
                btnUnequalSplit.setTextColor(Color.WHITE);
                btnEqualSplit.setBackgroundColor(Color.parseColor("#EFFFFD"));
                btnEqualSplit.setTextColor(Color.parseColor("#2D3142"));
                updateParticipantChecklist(containerParticipants, tvParticipatedHeader, false, allMemberUids);
            });
            
            // Close button
            btnClose.setOnClickListener(v -> dialog.dismiss());
            
            // Clear window flags and set soft input mode
            if (dialog.getWindow() != null) {
                int width = (int) (350 * getResources().getDisplayMetrics().density);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }

            // OCR Camera Scan button
            Button btnScanReceipt = dialogView.findViewById(R.id.btnScanReceipt);
            if (btnScanReceipt != null) {
                btnScanReceipt.setOnClickListener(v -> {
                    activeAmountEditText = etAmount;
                    launchCamera();
                });
            }

            btnAddExpense.setOnClickListener(v -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

                // Validate inputs
                if (title.isEmpty()) {
                    etTitle.setError("Title is required");
                    return;
                }
                if (amountStr.isEmpty()) {
                    etAmount.setError("Amount is required");
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    etAmount.setError("Invalid amount");
                    return;
                }

                // Get selected payer ΓÇö use the parallel uid list (safe even if name is a short UID fallback)
                int selectedPayerPos = spinnerWhoPaid.getSelectedItemPosition();
                String payerUid = (selectedPayerPos >= 0 && selectedPayerPos < whoPaidUids.size())
                        ? whoPaidUids.get(selectedPayerPos) : null;
                String selectedPayerName = (payerUid != null && memberNames.containsKey(payerUid))
                        ? memberNames.get(payerUid) : (String) spinnerWhoPaid.getSelectedItem();

                if (payerUid == null) {
                    Toast.makeText(this, "Please select who paid", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get selected participants
                List<String> participants = new ArrayList<>();
                for (Map.Entry<String, Boolean> entry : selectedParticipants.entrySet()) {
                    if (entry.getValue() != null && entry.getValue()) {
                        participants.add(entry.getKey());
                    }
                }
                
                // Get selected category
                String selectedCategory = spinnerCategory.getSelectedItem().toString();
                
                // Create expense object
                GroupExpense expense = new GroupExpense(
                    selectedGroupId[0], title, payerUid, selectedPayerName, amount
                );
                
                // Set additional properties
                expense.setParticipants(participants);
                expense.setSplitType(isEqualSplit ? "EQUAL" : "SELECTIVE");
                
                // Calculate and set split amounts for proper debt aggregation
                Map<String, Double> splitAmounts = new HashMap<>();
                if (isEqualSplit) {
                    // Equal split among all participants
                    double shareAmount = amount / participants.size();
                    shareAmount = Math.round(shareAmount * 100.0) / 100.0; // Round to 2 decimal places
                    
                    for (String participantUid : participants) {
                        splitAmounts.put(participantUid, shareAmount);
                    }
                } else {
                    // For selective split, we need to implement percentage/amount input logic
                    // For now, fall back to equal split among selected participants
                    double shareAmount = amount / participants.size();
                    shareAmount = Math.round(shareAmount * 100.0) / 100.0;
                    
                    for (String participantUid : participants) {
                        splitAmounts.put(participantUid, shareAmount);
                    }
                }
                expense.setSplitAmounts(splitAmounts);
                
                // Add expense to repository
                groupRepository.addGroupExpense(selectedGroupId[0], expense);
                
                Toast.makeText(this, "Expense added!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        } catch (Exception e) {
            Log.e("FAB_ERROR", "Crash in showAddExpenseDialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File storageDir = new File(getCacheDir(), "images");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            photoFile = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating file for image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private void updateParticipantChecklist(LinearLayout container, TextView header, boolean isEqualSplit, List<String> memberUids) {
        container.removeAllViews();
        selectedParticipants.clear();

        for (String memberUid : memberUids) {
            String memberName = memberNames.containsKey(memberUid)
                    ? memberNames.get(memberUid)
                    : memberUid.substring(0, Math.min(6, memberUid.length()));
            
            LinearLayout participantLayout = new LinearLayout(this);
            participantLayout.setOrientation(LinearLayout.HORIZONTAL);
            participantLayout.setPadding(8, 8, 8, 8);
            participantLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT));
            
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(memberName);
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            
            selectedParticipants.put(memberUid, false);
            
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectedParticipants.put(memberUid, isChecked);
                updateParticipatedHeader(header);
            });
            
            participantLayout.addView(checkBox);
            
            // Add percentage input for unequal split
            if (!isEqualSplit) {
                TextInputEditText percentageInput = new TextInputEditText(this);
                percentageInput.setHint("0%");
                percentageInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                percentageInput.setLayoutParams(new LinearLayout.LayoutParams(
                    120, LinearLayout.LayoutParams.WRAP_CONTENT));
                percentageInput.setTag(memberUid + "_percentage");
                participantLayout.addView(percentageInput);
            }
            
            container.addView(participantLayout);
        }
        
        updateParticipatedHeader(header);
    }
    
    private void updateParticipatedHeader(TextView header) {
        int selectedCount = 0;
        for (Boolean isSelected : selectedParticipants.values()) {
            if (isSelected != null && isSelected) {
                selectedCount++;
            }
        }
        header.setText("Who Participated? (" + selectedCount + " selected)");
    }

    private void loadGroupData() {
        // Load group data to check status and creator
        groupRepository.getGroups().observe(this, groups -> {
            if (groups != null) {
                for (Group group : groups) {
                    if (group.getId().equals(groupId)) {
                        currentGroup = group;
                        updateUI();
                        if (currentGroup.getMembers() != null) {
                            loadMemberNames(currentGroup.getMembers());
                        }
                        break;
                    }
                }
            }
        });
    }

private void updateUI() {
    if (currentGroup == null) return;

    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String currentUserId = currentUser != null ? currentUser.getUid() : null;

    // Show/hide Mark as Accomplished button based on creator and status
    if (currentUserId != null && currentUserId.equals(currentGroup.getCreatedBy()) 
            && currentGroup.isActive()) {
        btnMarkAsAccomplished.setVisibility(View.VISIBLE);
    } else {
        btnMarkAsAccomplished.setVisibility(View.GONE);
    }

    // Control FAB visibility based on group status
    if (currentGroup.isActive()) {
        if (fabAddExpense != null) {
            fabAddExpense.setVisibility(View.VISIBLE);
            Log.d("GroupLobby", "FAB made visible for active group: " + currentGroup.getName());
        }
    } else if (currentGroup.isSettled()) {
        // Disable functionality for settled groups
        disableAllInputElements();
        Log.d("GroupLobby", "FAB hidden for settled group: " + currentGroup.getName());
    }
}

private void markGroupAsAccomplished() {
    if (groupId == null || currentGroup == null) return;

    groupRepository.updateGroupStatus(groupId, "settled", new GroupRepository.OnCompleteCallback() {
        @Override
        public void onSuccess(String message) {
            Toast.makeText(GroupLobbyActivity.this, "Group marked as accomplished!", Toast.LENGTH_SHORT).show();
            // UI will update automatically when group data changes
        }

        @Override
        public void onError(String error) {
            Toast.makeText(GroupLobbyActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
        }
    });
}

private void disableAllInputElements() {
    // Disable Mark as Accomplished button
    if (btnMarkAsAccomplished != null) {
        btnMarkAsAccomplished.setEnabled(false);
        btnMarkAsAccomplished.setVisibility(View.GONE);
    }

    // Hide FAB for settled groups
    if (fabAddExpense != null) {
        fabAddExpense.setVisibility(View.GONE);
    }

    // Disable any other interactive elements if they exist
    // For now, the main restriction is preventing the add expense dialog
}

@Override
protected void onDestroy() {
    super.onDestroy();
    groupRepository.removeListeners();
    }
}

