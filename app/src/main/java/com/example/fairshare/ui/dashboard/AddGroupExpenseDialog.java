package com.example.fairshare.ui.dashboard;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.fairshare.CurrencyHelper;
import com.example.fairshare.Group;
import com.example.fairshare.GroupExpense;
import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized dialog builder for adding group expenses with flexible split types.
 * Supports: Equal, Selective (equal among subset), Percentage, and Exact Amount splits.
 */
public class AddGroupExpenseDialog {

    private final Context context;
    private final List<Group> availableGroups;
    private final OnExpenseSavedListener listener;
    private final GroupRepository groupRepository;
    private final DashboardViewModel viewModel;

    // Split type constants
    private static final String SPLIT_EQUAL = "EQUAL";
    private static final String SPLIT_SELECTIVE = "SELECTIVE";
    private static final String SPLIT_UNEQUAL = "UNEQUAL";

    private String currentSplitType = SPLIT_EQUAL;

    public interface OnExpenseSavedListener {
        void onExpenseSaved();
    }

    public AddGroupExpenseDialog(
            Context context,
            List<Group> availableGroups,
            DashboardViewModel viewModel,
            GroupRepository groupRepository,
            OnExpenseSavedListener listener) {
        this.context = context;
        this.availableGroups = availableGroups;
        this.viewModel = viewModel;
        this.groupRepository = groupRepository;
        this.listener = listener;
    }

    /**
     * Displays the "Add Group Expense" dialog with full functionality.
     */
    public void show() {
        if (availableGroups.isEmpty()) {
            Toast.makeText(context, "You are not part of any groups", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(context, R.style.Theme_FairShare_Dialog);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_group_expense, null);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        // Bind UI elements
        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        Spinner spinnerSelectGroup = dialogView.findViewById(R.id.spinnerSelectGroup);
        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        Spinner spinnerWhoPaid = dialogView.findViewById(R.id.spinnerWhoPaid);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        android.widget.LinearLayout containerParticipants = dialogView.findViewById(R.id.containerParticipants);
        MaterialButton btnAddExpense = dialogView.findViewById(R.id.btnAddExpense);
        android.widget.TextView tvParticipatedHeader = dialogView.findViewById(R.id.tvParticipatedHeader);
        MaterialButton btnEqualSplit = dialogView.findViewById(R.id.btnEqualSplit);
        MaterialButton btnItemized = dialogView.findViewById(R.id.btnItemized);

        // Setup group spinner
        String[] groupNames = new String[availableGroups.size()];
        for (int i = 0; i < availableGroups.size(); i++) {
            groupNames[i] = availableGroups.get(i).getName();
        }
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, groupNames);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelectGroup.setAdapter(groupAdapter);

        // Setup category spinner
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                context, R.array.categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // State for group selection
        final Group[] selectedGroup = {availableGroups.get(0)};
        final List<String> memberUids = new ArrayList<>();
        final List<String> memberNames = new ArrayList<>();
        final Map<String, android.widget.CheckBox> checkboxes = new HashMap<>();
        final Map<String, Double> customSplits = new HashMap<>(); // For unequal splits

        // Handle split type buttons
        currentSplitType = SPLIT_EQUAL;
        updateSplitTypeUI(btnEqualSplit, btnItemized, SPLIT_EQUAL);

        btnEqualSplit.setOnClickListener(v -> {
            currentSplitType = SPLIT_EQUAL;
            updateSplitTypeUI(btnEqualSplit, btnItemized, SPLIT_EQUAL);
            updateParticipantUI(containerParticipants, checkboxes, memberUids, memberNames, tvParticipatedHeader, etAmount, SPLIT_EQUAL, customSplits);
        });

        btnItemized.setOnClickListener(v -> {
            // Show dialog to choose between selective equal or unequal
            showSplitMethodDialog(context, newMethod -> {
                currentSplitType = newMethod;
                updateSplitTypeUI(btnEqualSplit, btnItemized, newMethod);
                updateParticipantUI(containerParticipants, checkboxes, memberUids, memberNames, tvParticipatedHeader, etAmount, newMethod, customSplits);
            });
        });

        // Handle group selection
        spinnerSelectGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGroup[0] = availableGroups.get(position);
                memberUids.clear();
                memberNames.clear();
                checkboxes.clear();
                customSplits.clear();
                containerParticipants.removeAllViews();
                spinnerWhoPaid.setAdapter(null);

                // Fetch group members
                viewModel.getGroupMembers(selectedGroup[0].getId(), uids -> {
                    for (String uid : uids) {
                        FirebaseFirestore.getInstance()
                                .collection("users").document(uid).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists() && dialog.isShowing()) {
                                        String name = doc.getString("displayName");
                                        if (name == null) name = "User";

                                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                        if (currentUser != null && currentUser.getUid().equals(uid)) {
                                            name = "You";
                                        }

                                        memberUids.add(uid);
                                        memberNames.add(name);

                                        // Update payer spinner
                                        ArrayAdapter<String> payerAdapter = new ArrayAdapter<>(
                                                context, android.R.layout.simple_spinner_item, new ArrayList<>(memberNames));
                                        payerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                        spinnerWhoPaid.setAdapter(payerAdapter);

                                        // Refresh participant UI
                                        updateParticipantUI(containerParticipants, checkboxes, memberUids, memberNames, tvParticipatedHeader, etAmount, currentSplitType, customSplits);

                                        // Set current user as payer
                                        if (name.equals("You")) {
                                            spinnerWhoPaid.setSelection(memberNames.size() - 1);
                                        }
                                    }
                                });
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Trigger initial group loading
        spinnerSelectGroup.setSelection(0);

        // Handle save button
        btnAddExpense.setOnClickListener(v -> {
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

            int selectedPayerIndex = spinnerWhoPaid.getSelectedItemPosition();
            if (selectedPayerIndex < 0) {
                Toast.makeText(context, "Please select who paid", Toast.LENGTH_SHORT).show();
                return;
            }

            String payerUid = memberUids.get(selectedPayerIndex);
            String payerName = memberNames.get(selectedPayerIndex);

            // Collect selected participants
            List<String> selectedParticipants = new ArrayList<>();
            for (String uid : checkboxes.keySet()) {
                android.widget.CheckBox cb = checkboxes.get(uid);
                if (cb != null && cb.isChecked()) {
                    selectedParticipants.add(uid);
                }
            }

            if (selectedParticipants.isEmpty()) {
                Toast.makeText(context, "Please select at least one participant", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate and calculate split amounts based on type
            Map<String, Double> splitAmounts = new HashMap<>();
            
            if (currentSplitType.equals(SPLIT_EQUAL) || currentSplitType.equals(SPLIT_SELECTIVE)) {
                // Equal split among selected participants
                double sharePerPerson = amount / selectedParticipants.size();
                for (String uid : selectedParticipants) {
                    splitAmounts.put(uid, sharePerPerson);
                }
            } else if (currentSplitType.equals(SPLIT_UNEQUAL)) {
                // Validate percentages add up to 100
                double totalPercent = 0;
                for (Double percent : customSplits.values()) {
                    if (percent != null) totalPercent += percent;
                }
                if (Math.abs(totalPercent - 100.0) > 0.01) {
                    Toast.makeText(context, "Percentages must add up to 100% (currently " + String.format("%.1f", totalPercent) + "%)", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                for (String uid : customSplits.keySet()) {
                    Double percent = customSplits.get(uid);
                    if (percent != null && percent > 0) {
                        splitAmounts.put(uid, Math.round((amount * percent / 100.0) * 100.0) / 100.0);
                    }
                }
            }

            // Create expense
            GroupExpense expense = new GroupExpense(
                    selectedGroup[0].getId(), title, payerUid, payerName, amount);
            expense.setParticipants(selectedParticipants);
            expense.setSplitType(currentSplitType);
            expense.setSplitAmounts(splitAmounts);

            viewModel.addGroupExpense(selectedGroup[0].getId(), expense);
            Toast.makeText(context, "Group Expense added!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            if (listener != null) {
                listener.onExpenseSaved();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    /**
     * Updates the split type UI button states.
     */
    private void updateSplitTypeUI(MaterialButton btnEqual, MaterialButton btnItemized, String splitType) {
        if (SPLIT_EQUAL.equals(splitType)) {
            btnEqual.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#38BDB0")));
            btnEqual.setTextColor(android.graphics.Color.WHITE);
            btnItemized.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EFFFFD")));
            btnItemized.setTextColor(android.graphics.Color.parseColor("#2D3142"));
        } else {
            btnEqual.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EFFFFD")));
            btnEqual.setTextColor(android.graphics.Color.parseColor("#2D3142"));
            btnItemized.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#38BDB0")));
            btnItemized.setTextColor(android.graphics.Color.WHITE);
        }
    }

    /**
     * Shows a dialog to choose between Selective Equal and Unequal splits.
     */
    private void showSplitMethodDialog(Context context, OnSplitMethodSelectedListener callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_FairShare_Dialog);
        builder.setTitle("Choose Split Method");
        builder.setItems(
                new String[]{"Selective Equal (equal among chosen members)", "Unequal (percentage-based)"},
                (dialog, which) -> {
                    if (which == 0) {
                        callback.onSelected(SPLIT_SELECTIVE);
                    } else {
                        callback.onSelected(SPLIT_UNEQUAL);
                    }
                }
        );
        builder.show();
    }

    /**
     * Updates participant UI based on split type.
     */
    private void updateParticipantUI(
            android.widget.LinearLayout container,
            Map<String, android.widget.CheckBox> checkboxes,
            List<String> memberUids,
            List<String> memberNames,
            android.widget.TextView tvHeader,
            TextInputEditText etAmount,
            String splitType,
            Map<String, Double> customSplits) {

        container.removeAllViews();
        checkboxes.clear();
        customSplits.clear();

        if (SPLIT_EQUAL.equals(splitType) || SPLIT_SELECTIVE.equals(splitType)) {
            // Show checkboxes for member selection
            for (int i = 0; i < memberUids.size(); i++) {
                final String uid = memberUids.get(i);
                String name = memberNames.get(i);

                android.widget.CheckBox cb = new android.widget.CheckBox(context);
                cb.setText(name);
                cb.setChecked(true);
                cb.setTextColor(android.graphics.Color.parseColor("#2D3142"));
                cb.setPadding(16, 24, 16, 24);
                cb.setTextSize(16);

                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    int count = 0;
                    for (android.widget.CheckBox c : checkboxes.values()) {
                        if (c.isChecked()) count++;
                    }
                    tvHeader.setText("Who Participated? (" + count + " selected)");
                });

                container.addView(cb);
                checkboxes.put(uid, cb);
            }
            tvHeader.setText("Who Participated? (" + memberUids.size() + " selected)");

        } else if (SPLIT_UNEQUAL.equals(splitType)) {
            // Show input fields for percentages
            tvHeader.setText("Percentage Distribution");
            
            for (int i = 0; i < memberUids.size(); i++) {
                final String uid = memberUids.get(i);
                String name = memberNames.get(i);

                android.widget.LinearLayout rowLayout = new android.widget.LinearLayout(context);
                rowLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                rowLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                rowLayout.setPadding(8, 8, 8, 8);

                android.widget.TextView tvName = new android.widget.TextView(context);
                tvName.setText(name);
                tvName.setTextColor(android.graphics.Color.parseColor("#2D3142"));
                tvName.setTextSize(14);
                tvName.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                EditText etPercent = new EditText(context);
                etPercent.setHint("0");
                etPercent.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                etPercent.setLayoutParams(new android.widget.LinearLayout.LayoutParams(100, ViewGroup.LayoutParams.WRAP_CONTENT));
                etPercent.setPadding(8, 8, 8, 8);
                etPercent.setTextSize(14);

                etPercent.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        try {
                            double percent = s.length() > 0 ? Double.parseDouble(s.toString()) : 0;
                            customSplits.put(uid, percent);
                        } catch (NumberFormatException e) {
                            customSplits.put(uid, 0.0);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

                android.widget.TextView tvPercent = new android.widget.TextView(context);
                tvPercent.setText("%");
                tvPercent.setTextSize(14);
                tvPercent.setLayoutParams(new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                rowLayout.addView(tvName);
                rowLayout.addView(etPercent);
                rowLayout.addView(tvPercent);

                container.addView(rowLayout);
                customSplits.put(uid, 0.0);
            }
        }
    }

    private interface OnSplitMethodSelectedListener {
        void onSelected(String splitType);
    }
}
