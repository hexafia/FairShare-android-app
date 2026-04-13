package com.example.fairshare.ui.groups;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.DebtSimplifier;
import com.example.fairshare.GroupExpense;
import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.example.fairshare.UserProfile;
import com.example.fairshare.UserRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupLobbyActivity extends AppCompatActivity {

    private String groupId;
    private String groupName;
    private String shareCode;

    private GroupRepository groupRepository;
    private UserRepository userRepository;
    private GroupExpenseAdapter expenseAdapter;
    private DebtAdapter debtAdapter;
    private MembersAdapter membersAdapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    private String createdBy;
    private String status;

    private TextView tvGroupTotal, tvMemberCount, tvMyBalance;
    private View layoutLedger, layoutSettleUp;
    private TextView tvLedgerEmpty, tvSettleEmpty;
    private RecyclerView rvLedger, rvDebts, rvMembers;
    private MaterialButton btnMarkAccomplished;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddExpense;
    private View layoutMembers;

    // Maps UID → display name for the Settle Up & Members tabs
    private final Map<String, String> memberNames = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_lobby);

        groupId = getIntent().getStringExtra("GROUP_ID");
        groupName = getIntent().getStringExtra("GROUP_NAME");
        shareCode = getIntent().getStringExtra("SHARE_CODE");
        createdBy = getIntent().getStringExtra("CREATED_BY");
        status = getIntent().getStringExtra("STATUS");

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
        rvLedger = findViewById(R.id.rvLedger);
        rvDebts = findViewById(R.id.rvDebts);
        rvMembers = findViewById(R.id.rvMembers);
        btnMarkAccomplished = findViewById(R.id.btnMarkAccomplished);
        fabAddExpense = findViewById(R.id.fabAddExpense);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                layoutLedger.setVisibility(View.GONE);
                layoutSettleUp.setVisibility(View.GONE);
                layoutMembers.setVisibility(View.GONE);

                if (tab.getPosition() == 0) {
                    layoutLedger.setVisibility(View.VISIBLE);
                } else if (tab.getPosition() == 1) {
                    layoutSettleUp.setVisibility(View.VISIBLE);
                } else {
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

        debtAdapter = new DebtAdapter();
        rvDebts.setLayoutManager(new LinearLayoutManager(this));
        rvDebts.setAdapter(debtAdapter);

        membersAdapter = new MembersAdapter();
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(membersAdapter);

        // FAB
        fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());

        // Repositories
        groupRepository = new GroupRepository();
        userRepository = new UserRepository();

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

        // Setup Mark as Accomplished button visibility & logic
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(createdBy) && !"settled".equals(status)) {
            btnMarkAccomplished.setVisibility(View.VISIBLE);
            btnMarkAccomplished.setOnClickListener(v -> markAsAccomplished());
        }

        if ("settled".equals(status)) {
            enforceReadonlyState();
        }
    }

    private void enforceReadonlyState() {
        btnMarkAccomplished.setVisibility(View.GONE);
        fabAddExpense.setVisibility(View.GONE);
        // Toast when trying to interact with anything disabled could be added here, 
        // but hiding the FAB is generally enough to prevent new expenses.
    }

    private void markAsAccomplished() {
        groupRepository.updateGroupStatus(groupId, "settled", new GroupRepository.OnCompleteCallback() {
            @Override
            public void onSuccess(String message) {
                status = "settled";
                Toast.makeText(GroupLobbyActivity.this, "Group settled", Toast.LENGTH_SHORT).show();
                enforceReadonlyState();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(GroupLobbyActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
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
                                    // Refresh debt & members display with new names
                                    debtAdapter.setMemberNames(memberNames);
                                    debtAdapter.notifyDataSetChanged();
                                    
                                    membersAdapter.setMembers(memberUids, memberNames);
                                    membersAdapter.notifyDataSetChanged();
                                }
                            }
                        });
            }
            
            // Initial render before names load
            membersAdapter.setMembers(memberUids, memberNames);
            membersAdapter.notifyDataSetChanged();
        });
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

        tvGroupTotal.setText(String.format("₱%,.0f", total));

        // My balance
        double myBalance = netBalances.getOrDefault(myUid, 0.0);
        if (myBalance >= 0) {
            tvMyBalance.setText(String.format("+₱%,.0f", myBalance));
        } else {
            tvMyBalance.setText(String.format("-₱%,.0f", Math.abs(myBalance)));
        }
    }

    private void updateDebts(List<GroupExpense> expenses) {
        List<DebtSimplifier.Debt> debts = DebtSimplifier.simplify(expenses);
        debtAdapter.setMemberNames(memberNames);
        debtAdapter.submitList(debts);

        if (debts.isEmpty()) {
            tvSettleEmpty.setVisibility(View.VISIBLE);
            rvDebts.setVisibility(View.GONE);
        } else {
            tvSettleEmpty.setVisibility(View.GONE);
            rvDebts.setVisibility(View.VISIBLE);
        }
    }

    private void showAddExpenseDialog() {
        Dialog dialog = new Dialog(this, R.style.Theme_FairShare_Dialog);
        dialog.setContentView(R.layout.dialog_add_group_expense);

        TextInputEditText etTitle = dialog.findViewById(R.id.etTitle);
        TextInputEditText etAmount = dialog.findViewById(R.id.etAmount);
        TextView tvSplitInfo = dialog.findViewById(R.id.tvSplitInfo);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSave);

        tvSplitInfo.setText("Split equally among all group members");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

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

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            String payerName = memberNames.getOrDefault(user.getUid(), "You");
            GroupExpense expense = new GroupExpense(groupId, title, user.getUid(), payerName, amount);

            // Add all current group members as participants
            for (String uid : memberNames.keySet()) {
                expense.getParticipants().add(uid);
            }
            // Also make sure payer is included
            if (!expense.getParticipants().contains(user.getUid())) {
                expense.getParticipants().add(user.getUid());
            }

            groupRepository.addGroupExpense(groupId, expense);
            Toast.makeText(this, "Expense added!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        groupRepository.removeListeners();
    }

    // Inner class for displaying Members
    private class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {
        private List<String> memberUids;
        private Map<String, String> nameMap;

        void setMembers(List<String> memberUids, Map<String, String> nameMap) {
            this.memberUids = memberUids;
            this.nameMap = nameMap;
        }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(32, 32, 32, 32);
            tv.setTextSize(16f);
            tv.setTextColor(0xFF2D3142); // @color/off_black
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT));
            return new MemberViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            String uid = memberUids.get(position);
            String name = nameMap.containsKey(uid) ? nameMap.get(uid) : "Loading...";
            if (createdBy != null && createdBy.equals(uid)) {
                name += " (Creator)";
            }
            ((TextView)holder.itemView).setText(name);
        }

        @Override
        public int getItemCount() {
            return memberUids != null ? memberUids.size() : 0;
        }

        class MemberViewHolder extends RecyclerView.ViewHolder {
            MemberViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
