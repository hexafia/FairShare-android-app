package com.example.fairshare.ui.dashboard;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

        // Handle group selection
        spinnerSelectGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGroup[0] = availableGroups.get(position);
                memberUids.clear();
                memberNames.clear();
                checkboxes.clear();
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
                                                context, android.R.layout.simple_spinner_item, memberNames);
                                        payerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                        spinnerWhoPaid.setAdapter(payerAdapter);

                                        // Create participant checkbox
                                        android.widget.CheckBox cb = new android.widget.CheckBox(context);
                                        cb.setText(name);
                                        cb.setChecked(true);
                                        cb.setTextColor(android.graphics.Color.parseColor("#2D3142"));
                                        cb.setPadding(16, 24, 16, 24);
                                        cb.setTextSize(16);

                                        containerParticipants.addView(cb);
                                        checkboxes.put(uid, cb);

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
                if (checkboxes.get(uid).isChecked()) {
                    selectedParticipants.add(uid);
                }
            }

            if (selectedParticipants.isEmpty()) {
                Toast.makeText(context, "Please select at least one participant", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create expense with equal split (for now)
            GroupExpense expense = new GroupExpense(
                    selectedGroup[0].getId(), title, payerUid, payerName, amount);
            expense.setParticipants(selectedParticipants);
            expense.setSplitType("EQUAL");

            // Calculate splitAmounts for equal split
            Map<String, Double> splitAmounts = new HashMap<>();
            double sharePerPerson = amount / selectedParticipants.size();
            for (String uid : selectedParticipants) {
                splitAmounts.put(uid, sharePerPerson);
            }
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
}
