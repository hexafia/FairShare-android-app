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
            
            // 1. SPINNER SELECT GROUP INITIALIZATION
            // Fetch only active groups for the spinner
            groupRepository.getGroups().observe(this, groups -> {
                if (groups != null) {
                    ArrayList<String> activeGroupNames = new ArrayList<>();
                    ArrayList<String> activeGroupIds = new ArrayList<>();
                    
                    // Filter out archived groups (status != "active")
                    for (Group group : groups) {
                        if (group.isActive()) {
                            activeGroupNames.add(group.getName());
                            activeGroupIds.add(group.getId());
                        }
                    }
                    
                    // Create adapter for spinner
                    ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, activeGroupNames);
                    groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSelectGroup.setAdapter(groupAdapter);
                    spinnerSelectGroup.setClickable(true);
                    spinnerSelectGroup.setEnabled(true);
                    
                    // Default to current group if it's active
                    int currentPosition = activeGroupIds.indexOf(groupId);
                    if (currentPosition >= 0) {
                        spinnerSelectGroup.setSelection(currentPosition);
                    }
                    
                    // Set up selection listener to sync groupId
                    spinnerSelectGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedGroupId[0] = activeGroupIds.get(position);
                            // Reload member names for the selected group
                            loadMemberNamesForGroup(selectedGroupId[0]);
                        }
                        
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
            });
            
            // Touch event fix for spinnerSelectGroup
            spinnerSelectGroup.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
            
            // 2. WHO PAID SPINNER INITIALIZATION
            // Who Paid Spinner - populate with group members
            ArrayList<String> memberNamesList = new ArrayList<>(memberNames.values());
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
            
            // 3. TOUCH EVENT FIX FOR NESTEDSCROLLVIEW
            spinnerWhoPaid.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
            
            spinnerCategory.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
            
            // 4. PARTICIPANT CHECKLIST (Dynamic Inflation)
            selectedParticipants.clear();
            updateParticipantChecklist(containerParticipants, tvParticipatedHeader, true);
            
            // 5. EQUAL/UNEQUAL SPLIT TOGGLE
            btnEqualSplit.setOnClickListener(v -> {
                btnEqualSplit.setBackgroundColor(Color.parseColor("#38BDB0"));
                btnEqualSplit.setTextColor(Color.WHITE);
                btnUnequalSplit.setBackgroundColor(Color.parseColor("#EFFFFD"));
                btnUnequalSplit.setTextColor(Color.parseColor("#2D3142"));
                updateParticipantChecklist(containerParticipants, tvParticipatedHeader, true);
            });
            
            btnUnequalSplit.setOnClickListener(v -> {
                btnUnequalSplit.setBackgroundColor(Color.parseColor("#38BDB0"));
                btnUnequalSplit.setTextColor(Color.WHITE);
                btnEqualSplit.setBackgroundColor(Color.parseColor("#EFFFFD"));
                btnEqualSplit.setTextColor(Color.parseColor("#2D3142"));
                updateParticipantChecklist(containerParticipants, tvParticipatedHeader, false);
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

                // Get selected payer
                String selectedPayerName = (String) spinnerWhoPaid.getSelectedItem();
                String payerUid = null;
                for (Map.Entry<String, String> entry : memberNames.entrySet()) {
                    if (entry.getValue().equals(selectedPayerName)) {
                        payerUid = entry.getKey();
                        break;
                    }
                }

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

                if (participants.isEmpty()) {
                    Toast.makeText(this, "Please select at least one participant", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create expense
                String category = (String) spinnerCategory.getSelectedItem();
                GroupExpense expense = new GroupExpense(selectedGroupId[0], title, payerUid, selectedPayerName, amount);
                expense.setParticipants(participants);
                
                // Save the expense
                groupRepository.addGroupExpense(selectedGroupId[0], expense);
                
                Toast.makeText(this, "Expense added!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        } catch (Exception e) {
            Log.e("FAB_ERROR", "Crash in showAddExpenseDialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadMemberNamesForGroup(String targetGroupId) {
        // Clear current member names
        memberNames.clear();
        selectedParticipants.clear();
        
        // Load member names for the selected group
        groupRepository.getGroupMembers(targetGroupId, memberUids -> {
            for (String uid : memberUids) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("displayName");
                                if (name != null) {
                                    memberNames.put(uid, name);
                                    // Refresh settlement display with new names
                                    if (settlementAdapter != null) {
                                        settlementAdapter.setMemberNames(memberNames);
                                        settlementAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        });
            }
        });
    }
    
    private void updateParticipantChecklist(LinearLayout container, TextView header, boolean isEqualSplit) {
        container.removeAllViews();
        selectedParticipants.clear();
        
        for (Map.Entry<String, String> entry : memberNames.entrySet()) {
            String memberUid = entry.getKey();
            String memberName = entry.getValue();
            
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

