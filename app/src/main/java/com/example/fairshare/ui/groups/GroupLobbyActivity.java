package com.example.fairshare.ui.groups;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.FairShareApp;
import com.example.fairshare.ContributionSummary;
import com.example.fairshare.GroupExpense;
import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.example.fairshare.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupLobbyActivity extends AppCompatActivity {

    private String groupId;
    private String groupName;
    private String shareCode;

    private GroupRepository groupRepository;
    private GroupExpenseAdapter expenseAdapter;
    private ContributionAdapter contributionAdapter;

    private TextView tvGroupTotal, tvMemberCount, tvMyBalance;
    private View layoutLedger, layoutSettleUp;
    private TextView tvLedgerEmpty, tvSettleEmpty;
    private RecyclerView rvLedger, rvDebts;
    private ContributionPieChartView pieChartView;
    private TextView tvEquityHeadline;
    private final List<String> groupMemberUids = new ArrayList<>();

    // Maps UID → display name for the equity tab
    private final Map<String, String> memberNames = new HashMap<>();

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
        tvShareCode.setText("Code: " + shareCode);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup tabs
        layoutLedger = findViewById(R.id.layoutLedger);
        layoutSettleUp = findViewById(R.id.layoutSettleUp);
        tvLedgerEmpty = findViewById(R.id.tvLedgerEmpty);
        tvSettleEmpty = findViewById(R.id.tvSettleEmpty);
        rvLedger = findViewById(R.id.rvLedger);
        rvDebts = findViewById(R.id.rvDebts);
        pieChartView = findViewById(R.id.pieChartView);
        tvEquityHeadline = findViewById(R.id.tvEquityHeadline);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutLedger.setVisibility(View.VISIBLE);
                    layoutSettleUp.setVisibility(View.GONE);
                } else {
                    layoutLedger.setVisibility(View.GONE);
                    layoutSettleUp.setVisibility(View.VISIBLE);
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

        contributionAdapter = new ContributionAdapter();
        rvDebts.setLayoutManager(new LinearLayoutManager(this));
        rvDebts.setAdapter(contributionAdapter);

        // FAB
        findViewById(R.id.fabAddExpense).setOnClickListener(v -> showAddExpenseDialog());

        // Repositories
        groupRepository = new GroupRepository();

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
                pieChartView.setContributions(Collections.emptyList());
                tvEquityHeadline.setText(getString(R.string.equity_empty_summary));
            } else {
                tvLedgerEmpty.setVisibility(View.GONE);
                rvLedger.setVisibility(View.VISIBLE);
                List<ContributionSummary> summaries = buildContributionSummaries(expenses);
                updateStats(expenses, summaries);
                updateEquityBreakdown(summaries);
            }
        });
    }

    private void loadMemberNames() {
        groupRepository.getGroupMembers(groupId, memberUids -> {
            groupMemberUids.clear();
            groupMemberUids.addAll(memberUids);
            tvMemberCount.setText(String.valueOf(memberUids.size()));

            for (String uid : memberUids) {
                if ("local-admin".equals(uid)) {
                    memberNames.put(uid, "admin");
                }
            }

            // For each member UID, look up their display name from the users Firestore collection
            for (String uid : memberUids) {
                if ("local-admin".equals(uid)) {
                    continue;
                }
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("displayName");
                                if (name != null) {
                                    memberNames.put(uid, name);
                                    contributionAdapter.notifyDataSetChanged();
                                }
                            }
                        });
            }
        });
    }

    private void updateStats(List<GroupExpense> expenses, List<ContributionSummary> summaries) {
        double total = 0;
        String myUid = SessionManager.getCurrentUid(FairShareApp.getAppContext());
        if (myUid == null) {
            myUid = "";
        }

        for (GroupExpense e : expenses) {
            total += e.getContributionTotal();
        }

        tvGroupTotal.setText(String.format(Locale.US, "₱%,.2f", total));

        for (ContributionSummary summary : summaries) {
            if (summary.getUid().equals(myUid)) {
                tvMyBalance.setText(String.format(Locale.US, "₱%,.2f", summary.getAmount()));
                return;
            }
        }
        tvMyBalance.setText("₱0");
    }

    private void updateEquityBreakdown(List<ContributionSummary> summaries) {
        contributionAdapter.submitList(summaries);
        pieChartView.setContributions(summaries);

        if (summaries.isEmpty()) {
            tvSettleEmpty.setVisibility(View.VISIBLE);
            rvDebts.setVisibility(View.GONE);
            pieChartView.setVisibility(View.GONE);
            tvEquityHeadline.setText(getString(R.string.equity_empty_summary));
        } else {
            tvSettleEmpty.setVisibility(View.GONE);
            rvDebts.setVisibility(View.VISIBLE);
            pieChartView.setVisibility(View.VISIBLE);
            ContributionSummary topContributor = summaries.get(0);
            tvEquityHeadline.setText(getString(
                    R.string.equity_top_summary,
                    topContributor.getName(),
                    String.format(Locale.US, "%.1f", topContributor.getPercentage())
            ));
        }
    }

    private List<ContributionSummary> buildContributionSummaries(List<GroupExpense> expenses) {
        Map<String, Double> totalsByMember = new HashMap<>();
        double total = 0d;

        for (GroupExpense expense : expenses) {
            Map<String, Double> contributionMap = expense.getContributionAmounts();
            if (contributionMap != null && !contributionMap.isEmpty()) {
                for (Map.Entry<String, Double> entry : contributionMap.entrySet()) {
                    String uid = entry.getKey();
                    Double value = entry.getValue();
                    if (uid == null || uid.isEmpty() || value == null || value <= 0d) {
                        continue;
                    }
                    total += value;
                    totalsByMember.put(uid, totalsByMember.getOrDefault(uid, 0d) + value);
                }
                continue;
            }

            String uid = expense.getPayerUid();
            if (uid == null || uid.isEmpty()) {
                continue;
            }
            total += expense.getAmount();
            totalsByMember.put(uid, totalsByMember.getOrDefault(uid, 0d) + expense.getAmount());
        }

        if (total <= 0d) {
            return Collections.emptyList();
        }

        List<ContributionSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : totalsByMember.entrySet()) {
            String uid = entry.getKey();
            double amount = entry.getValue();
            double percentage = (amount / total) * 100d;
            String name = memberNames.getOrDefault(uid, uid);
            summaries.add(new ContributionSummary(uid, name, amount, percentage));
        }

        summaries.sort(Comparator.comparingDouble(ContributionSummary::getAmount).reversed());
        return summaries;
    }

    private void showAddExpenseDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_group_expense);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText etTitle = dialog.findViewById(R.id.etTitle);
        LinearLayout layoutContributionInputs = dialog.findViewById(R.id.layoutContributionInputs);
        TextView tvSplitInfo = dialog.findViewById(R.id.tvSplitInfo);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSave);
        Map<String, TextInputEditText> amountInputs = new HashMap<>();
        Map<String, TextInputLayout> amountLayouts = new HashMap<>();

        tvSplitInfo.setText(getString(R.string.equity_contribution_info));
        renderContributionInputs(layoutContributionInputs, amountInputs, amountLayouts);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }

            String uid = SessionManager.getCurrentUid(FairShareApp.getAppContext());
            if (uid == null) return;

            Map<String, Double> contributions = new HashMap<>();
            double totalAmount = 0d;

            for (String memberUid : amountInputs.keySet()) {
                TextInputEditText input = amountInputs.get(memberUid);
                TextInputLayout inputLayout = amountLayouts.get(memberUid);
                if (inputLayout != null) {
                    inputLayout.setError(null);
                }

                String raw = input != null && input.getText() != null
                        ? input.getText().toString().trim()
                        : "";
                if (TextUtils.isEmpty(raw)) {
                    continue;
                }

                double contribution;
                try {
                    contribution = Double.parseDouble(raw);
                } catch (NumberFormatException e) {
                    if (inputLayout != null) {
                        inputLayout.setError("Invalid amount");
                    }
                    return;
                }

                if (contribution < 0d) {
                    if (inputLayout != null) {
                        inputLayout.setError("Amount must be 0 or higher");
                    }
                    return;
                }

                if (contribution > 0d) {
                    contributions.put(memberUid, contribution);
                    totalAmount += contribution;
                }
            }

            if (contributions.isEmpty()) {
                Toast.makeText(this, "Add at least one contribution amount", Toast.LENGTH_SHORT).show();
                return;
            }

            String payerName = memberNames.getOrDefault(uid, "admin");
            GroupExpense expense = new GroupExpense(title, uid, payerName, totalAmount);
            expense.setContributionAmounts(contributions);
            for (String contributorUid : contributions.keySet()) {
                expense.getParticipants().put(contributorUid, true);
            }

            groupRepository.addGroupExpense(groupId, expense);
            Toast.makeText(this, "Contribution ledger saved!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void renderContributionInputs(
            LinearLayout container,
            Map<String, TextInputEditText> amountInputs,
            Map<String, TextInputLayout> amountLayouts
    ) {
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        List<String> sortedMemberUids = new ArrayList<>(groupMemberUids);
        Collections.sort(sortedMemberUids, Comparator.comparing(uid -> memberNames.getOrDefault(uid, uid).toLowerCase(Locale.US)));

        for (String memberUid : sortedMemberUids) {
            View row = inflater.inflate(R.layout.item_member_contribution_input, container, false);
            TextView tvMemberName = row.findViewById(R.id.tvMemberName);
            TextInputEditText etMemberAmount = row.findViewById(R.id.etMemberAmount);
            TextInputLayout tilMemberAmount = (TextInputLayout) etMemberAmount.getParent().getParent();

            tvMemberName.setText(memberNames.getOrDefault(memberUid, memberUid));
            amountInputs.put(memberUid, etMemberAmount);
            amountLayouts.put(memberUid, tilMemberAmount);
            container.addView(row);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        groupRepository.removeListeners();
    }
}
