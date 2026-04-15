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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
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
import java.util.HashSet;
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
import com.example.fairshare.ui.dashboard.AddGroupExpenseDialog;
import com.example.fairshare.Constants;
import com.example.fairshare.FastActionHandler;
import com.example.fairshare.Notification;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;

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
    
        
    // Store current expenses list for tab refresh
    private List<GroupExpense> currentExpensesList = new ArrayList<>();
    private List<GroupExpense> filteredExpensesList = new ArrayList<>();
    private androidx.appcompat.widget.SearchView searchViewLedger;
    private ImageButton btnFilterDropdown;
    
    // Filter state
    private String currentSortOption = "date_newest"; // "date_newest", "date_oldest", "category", "payer"
    private String currentCategoryFilter = null;
    private String currentPayerFilter = null;

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
        
        // Handle openSettleUpTab intent from notifications
        boolean openSettleUpTab = getIntent().getBooleanExtra("openSettleUpTab", false);

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
        searchViewLedger = findViewById(R.id.searchViewLedger);
        btnFilterDropdown = findViewById(R.id.btnFilterDropdown);
        
        // Setup search and filter functionality
        setupSearchView();
        setupFilterDropdown();
        
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
        
        // Auto-open Settle Up tab if requested (e.g., from notification)
        if (openSettleUpTab) {
            tabLayout.selectTab(tabLayout.getTabAt(1)); // Select Settle Up tab (index 1)
        }

        // Setup RecyclerViews
        expenseAdapter = new GroupExpenseAdapter();
        rvLedger.setLayoutManager(new LinearLayoutManager(this));
        rvLedger.setAdapter(expenseAdapter);

        // Setup two-section adapters
        toPayAdapter = new SettlementDetailAdapter();
        paidAdapter = new SettlementDetailAdapter();
        
        // Set current user ID for adapters
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                              FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        toPayAdapter.setCurrentUserId(currentUserId);
        paidAdapter.setCurrentUserId(currentUserId);
        
        // Settle click listener for "To Pay" section
        toPayAdapter.setOnSettleClickListener(settlement -> {
            // Check if current user is the original payer (can confirm settlements)
            if (!currentUserId.equals(settlement.payerUid)) {
                Toast.makeText(this, "Only the original payer can confirm this settlement", Toast.LENGTH_SHORT).show();
                return;
            }
            
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
                                updateDebts(currentExpensesList);
                                
                                // Create payment confirmation notification
                                createPaymentConfirmationNotification(settlement);
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
        
        // Nudge click listener for "To Pay" section
        toPayAdapter.setOnNudgeClickListener(settlement -> {
            // Only the original payer (who is owed money) can nudge the debtor
            if (!currentUserId.equals(settlement.payerUid)) {
                Toast.makeText(this, "Only the person who is owed money can send a nudge", Toast.LENGTH_SHORT).show();
                return;
            }
            
            sendNudge(settlement.debtorUid, settlement.settlementAmount, settlement.expenseTitle, groupName);
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
                // Create list with current group for AddGroupExpenseDialog
                List<Group> currentGroupList = new ArrayList<>();
                if (currentGroup != null) {
                    currentGroupList.add(currentGroup);
                }
                
                // Create dialog with proper constructor
                AddGroupExpenseDialog dialog = new AddGroupExpenseDialog(
                        this,
                        currentGroupList,
                        null, // DashboardViewModel - not needed in GroupLobbyActivity
                        groupRepository,
                        () -> {
                            // Callback to refresh ledger after expense saved
                            Log.d("FAB_DEBUG", "Expense saved, refreshing expenses");
                            // The expense observer will automatically refresh the ledger
                        });
                dialog.show();
            } catch (Exception e) {
                Log.e("FAB_ERROR", "Error in AddGroupExpenseDialog: " + e.getMessage());
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
            filteredExpensesList = new ArrayList<>(currentExpensesList);
            
            // Update expense adapter with current filter
            String currentQuery = searchViewLedger != null ? searchViewLedger.getQuery().toString() : "";
            if (currentQuery.trim().isEmpty()) {
                expenseAdapter.submitList(currentExpensesList);
            } else {
                filterExpenses(currentQuery);
            }
            
            // Always apply filters when new expenses arrive
            if (currentCategoryFilter != null && !currentCategoryFilter.isEmpty()) {
                filterExpenses(""); // Re-apply category filter
            } else if (currentPayerFilter != null && !currentPayerFilter.isEmpty()) {
                filterExpenses(""); // Re-apply payer filter
            }

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
                                // Update expense adapter with member names to fix "Unknown User" issue
                                if (expenseAdapter != null) {
                                    expenseAdapter.setMemberNames(memberNames);
                                    expenseAdapter.notifyDataSetChanged();
                                }
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
            
            // Check if group is accomplished and set read-only state
            boolean isGroupAccomplished = currentGroup != null && currentGroup.isSettled();
            toPayAdapter.setGroupAccomplished(isGroupAccomplished);
            paidAdapter.setGroupAccomplished(isGroupAccomplished);
            
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
    
    private void setupSearchView() {
        searchViewLedger.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterExpenses(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterExpenses(newText);
                return true;
            }
        });
    }

    private void filterExpenses(String query) {
        // Start with all expenses
        List<GroupExpense> tempList = new ArrayList<>(currentExpensesList);
        
        // Apply search query filter
        if (query != null && !query.trim().isEmpty()) {
            String searchQuery = query.toLowerCase().trim();
            List<GroupExpense> searchFiltered = new ArrayList<>();
            
            for (GroupExpense expense : tempList) {
                boolean matches = false;
                
                // Search by expense title
                if (expense.getTitle() != null && expense.getTitle().toLowerCase().contains(searchQuery)) {
                    matches = true;
                }
                
                // Search by payer name
                if (expense.getPayerName() != null && expense.getPayerName().toLowerCase().contains(searchQuery)) {
                    matches = true;
                }
                
                // Search by category
                if (expense.getCategory() != null && expense.getCategory().toLowerCase().contains(searchQuery)) {
                    matches = true;
                }
                
                // Search by date (format: MMM dd, yyyy)
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String dateStr = dateFormat.format(new Date(expense.getTimestamp()));
                if (dateStr.toLowerCase().contains(searchQuery)) {
                    matches = true;
                }
                
                if (matches) {
                    searchFiltered.add(expense);
                }
            }
            tempList = searchFiltered;
        }
        
        // Apply category filter
        if (currentCategoryFilter != null && !currentCategoryFilter.isEmpty()) {
            List<GroupExpense> categoryFiltered = new ArrayList<>();
            for (GroupExpense expense : tempList) {
                String expenseCategory = expense.getCategory();
                Log.d("CATEGORY_FILTER", "Expense: " + expense.getTitle() + ", Category: " + expenseCategory + ", Filter: " + currentCategoryFilter);
                
                // Handle null category and case-insensitive comparison
                if (expenseCategory != null && currentCategoryFilter.equalsIgnoreCase(expenseCategory)) {
                    categoryFiltered.add(expense);
                } else if (expenseCategory == null) {
                    // Include expenses without category when "All Categories" isn't selected
                    // This handles cases where expenses were created before category field was added
                    categoryFiltered.add(expense);
                }
            }
            tempList = categoryFiltered;
            Log.d("CATEGORY_FILTER", "Filtered to " + categoryFiltered.size() + " expenses out of " + tempList.size() + " (null category: " + 
                + tempList.stream().mapToLong(e -> e.getCategory() == null ? 1L : 0L).sum() + ")");
        }
        
        // Apply payer filter
        if (currentPayerFilter != null && !currentPayerFilter.isEmpty()) {
            List<GroupExpense> payerFiltered = new ArrayList<>();
            for (GroupExpense expense : tempList) {
                if (currentPayerFilter.equals(expense.getPayerName())) {
                    payerFiltered.add(expense);
                }
            }
            tempList = payerFiltered;
        }
        
        // Apply sorting
        switch (currentSortOption) {
            case "date_newest":
                tempList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                break;
            case "date_oldest":
                tempList.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                break;
            case "payer":
                // Already filtered by payer, maintain date order
                tempList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                break;
        }
        
        filteredExpensesList = tempList;
        
        // Update the expense adapter with filtered results
        if (expenseAdapter != null) {
            expenseAdapter.submitList(filteredExpensesList);
            
            // Update empty state
            if (filteredExpensesList.isEmpty()) {
                tvLedgerEmpty.setVisibility(View.VISIBLE);
                rvLedger.setVisibility(View.GONE);
            } else {
                tvLedgerEmpty.setVisibility(View.GONE);
                rvLedger.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupFilterDropdown() {
        btnFilterDropdown.setOnClickListener(v -> showFilterPopupMenu());
    }

    private void showFilterPopupMenu() {
        PopupMenu popup = new PopupMenu(this, btnFilterDropdown);
        popup.getMenuInflater().inflate(R.menu.filter_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_sort_date_newest) {
                currentSortOption = "date_newest";
                applyFilters();
            } else if (itemId == R.id.action_sort_date_oldest) {
                currentSortOption = "date_oldest";
                applyFilters();
            } else if (itemId == R.id.action_filter_payer) {
                showPayerFilterDialog();
            } else if (itemId == R.id.action_clear_filters) {
                currentPayerFilter = null;
                currentCategoryFilter = null;
                currentSortOption = "date_newest";
                applyFilters();
            }
            return true;
        });

        popup.show();
    }

    
    private void showPayerFilterDialog() {
        // Get unique payers from expenses
        Set<String> payers = new HashSet<>();
        for (GroupExpense expense : currentExpensesList) {
            if (expense.getPayerName() != null) {
                payers.add(expense.getPayerName());
            }
        }
        
        String[] payerArray = payers.toArray(new String[0]);
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Filter by Payer");
        builder.setItems(payerArray, (dialog, which) -> {
            currentPayerFilter = payerArray[which];
            currentSortOption = "payer";
            currentCategoryFilter = null;
            applyFilters();
        });
        builder.show();
    }

    private void showCategoryFilterDialog() {
        // Get unique categories from expenses
        Set<String> categories = new HashSet<>();
        for (GroupExpense expense : currentExpensesList) {
            if (expense.getCategory() != null && !expense.getCategory().trim().isEmpty()) {
                categories.add(expense.getCategory());
            }
        }
        
        String[] categoryArray = categories.toArray(new String[0]);
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Filter by Category");
        builder.setItems(categoryArray, (dialog, which) -> {
            currentCategoryFilter = categoryArray[which];
            currentSortOption = "category";
            currentPayerFilter = null;
            applyFilters();
        });
        builder.show();
    }

    private void applyFilters() {
        String currentQuery = searchViewLedger != null ? searchViewLedger.getQuery().toString() : "";
        filterExpenses(currentQuery);
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

    private void sendNudge(String debtorUid, double amount, String expenseName, String groupName) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                              FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        String currentUserName = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                               FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "Someone";
        
        if (currentUserId.isEmpty() || debtorUid.equals(currentUserId)) {
            Toast.makeText(this, "Cannot send nudge to yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create notification document in Firestore with proper text format
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("recipientUid", debtorUid);
        notificationData.put("senderName", currentUserName);
        notificationData.put("senderUid", currentUserId);
        notificationData.put("amount", amount);
        notificationData.put("expenseName", expenseName);
        notificationData.put("groupName", groupName);
        notificationData.put("groupId", groupId);
        notificationData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        notificationData.put("type", "nudge");
        notificationData.put("isRead", false);
        // Create message with required format: <Name> nudged you. You still have a balance of Php<Amount> for <Expense> in <Group>
        String message = currentUserName + " nudged you. You still have a balance of Php" + 
                         String.format("%.2f", amount) + " for " + expenseName + " in " + groupName;
        notificationData.put("message", message);

        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(this, "Nudge sent successfully!", Toast.LENGTH_SHORT).show();
                Log.d("NOTIFICATION", "Nudge notification created with ID: " + documentReference.getId());
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to send nudge: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("NOTIFICATION", "Failed to create nudge notification", e);
            });
    }

    private void createPaymentConfirmationNotification(SettlementCalculator.SettlementDetail settlement) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                              FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        String currentUserName = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                               FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "Someone";
        
        if (currentUserId.isEmpty()) {
            return;
        }

        // Only the original Payer can confirm/settle the debt
        // The sender is the Payer (person who is owed money and confirms payment)
        // The recipient is the debtor (person who paid)
        if (!currentUserId.equals(settlement.payerUid)) {
            Log.d("NOTIFICATION", "Only the original Payer can confirm this settlement");
            return;
        }
        
        String recipientUid = settlement.debtorUid;
        
        // Don't send notification to yourself (shouldn't happen but just in case)
        if (recipientUid.equals(currentUserId)) {
            return;
        }

        // Create payment confirmation notification document in Firestore with proper text format
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("recipientUid", recipientUid);
        notificationData.put("senderName", currentUserName);
        notificationData.put("senderUid", currentUserId);
        notificationData.put("amount", settlement.settlementAmount);
        notificationData.put("expenseName", settlement.expenseTitle);
        notificationData.put("expenseId", settlement.expenseId);
        notificationData.put("groupName", groupName);
        notificationData.put("groupId", groupId);
        notificationData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        notificationData.put("type", "payment_confirmed");
        notificationData.put("isRead", false);
        // Create message with required format: <Name> has confirmed your payment for <Expense> in <Group> worth Php<Amount>
        String message = currentUserName + " has confirmed your payment for " + 
                         settlement.expenseTitle + " in " + groupName + " worth Php" + String.format("%.2f", settlement.settlementAmount);
        notificationData.put("message", message);

        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener(documentReference -> {
                Log.d("NOTIFICATION", "Payment confirmation notification created with ID: " + documentReference.getId());
            })
            .addOnFailureListener(e -> {
                Log.e("NOTIFICATION", "Failed to create payment confirmation notification", e);
            });
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

