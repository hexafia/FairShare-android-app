package com.example.fairshare.ui.dashboard;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fairshare.DebtSimplifier;
import com.example.fairshare.ExpenseAdapter;
import com.example.fairshare.GroupExpense;
import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.example.fairshare.Transaction;
import com.example.fairshare.databinding.FragmentDashboardBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private ExpenseAdapter adapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    private List<Transaction> currentPersonalExpenses = new ArrayList<>();
    private List<GroupExpense> currentGroupExpenses = new ArrayList<>();
    private List<com.example.fairshare.Group> currentGroups = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupViewModel();

        binding.fabAdd.setOnClickListener(v -> showTransactionTypeDialog());
        binding.btnDashboardAddExpense.setOnClickListener(v -> showTransactionTypeDialog());
        binding.btnDashboardNewGroup.setOnClickListener(v -> showCreateGroupDialog());
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(this::showDeleteConfirmation);
        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExpenses.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        viewModel.getPersonalExpenses().observe(getViewLifecycleOwner(), transactions -> {
            currentPersonalExpenses = transactions != null ? transactions : new ArrayList<>();
            adapter.submitList(currentPersonalExpenses);
            updateSummary();

            if (currentPersonalExpenses.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.rvExpenses.setVisibility(View.GONE);
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                binding.rvExpenses.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getGroupExpenses().observe(getViewLifecycleOwner(), expenses -> {
            currentGroupExpenses = expenses != null ? expenses : new ArrayList<>();
            updateSummary();
        });

        viewModel.getGroups().observe(getViewLifecycleOwner(), groups -> {
            currentGroups = groups != null ? groups : new ArrayList<>();
        });
    }

    private boolean isCurrentMonth(Date date) {
        if (date == null) return false;
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.MONTH) == target.get(Calendar.MONTH);
    }

    private boolean isCurrentMonth(long timestamp) {
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.MONTH) == target.get(Calendar.MONTH);
    }

    private void updateSummary() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String myUid = user.getUid();

        double personalSpent = 0;
        for (Transaction t : currentPersonalExpenses) {
            if (isCurrentMonth(t.getDate()) && "expense".equalsIgnoreCase(t.getType())) {
                personalSpent += t.getAmount();
            }
        }

        List<GroupExpense> monthGroupExpenses = new ArrayList<>();
        double groupPaid = 0;
        for (GroupExpense ge : currentGroupExpenses) {
            if (isCurrentMonth(ge.getTimestamp())) {
                monthGroupExpenses.add(ge);
                if (myUid.equals(ge.getPayerUid())) {
                    groupPaid += ge.getAmount();
                }
            }
        }

        double remainingOwed = 0;
        List<DebtSimplifier.Debt> debts = DebtSimplifier.simplify(monthGroupExpenses);
        for (DebtSimplifier.Debt d : debts) {
            if (d.debtorUid.equals(myUid)) {
                remainingOwed += d.amount;
            }
        }

        double totalBalance = personalSpent + groupPaid + remainingOwed;

        binding.tvBalance.setText(currencyFormat.format(totalBalance));
        binding.tvPersonal.setText(currencyFormat.format(personalSpent));
        binding.tvPaid.setText(currencyFormat.format(groupPaid));
        binding.tvRemaining.setText(currencyFormat.format(remainingOwed));
    }

    private void showTransactionTypeDialog() {
        CharSequence[] options = new CharSequence[]{"Personal Expense", "Group Expense"};
        new AlertDialog.Builder(requireContext(), R.style.Theme_FairShare_Dialog)
                .setTitle("Select type of transaction")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddDialog();
                    } else {
                        showAddGroupExpenseDialog();
                    }
                })
                .show();
    }

    // This handles the regular personal transaction dialog
    private void showAddDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null);

        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        MaterialButtonToggleGroup toggleType = dialogView.findViewById(R.id.toggleType);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        toggleType.check(R.id.btnExpense);

        Dialog dialog = new Dialog(requireContext(), R.style.Theme_FairShare_Dialog);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

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

            String category = spinnerCategory.getSelectedItem().toString();
            String type = toggleType.getCheckedButtonId() == R.id.btnIncome ? "income" : "expense";

            Transaction transaction = new Transaction(title, amount, category, type);
            viewModel.addPersonalExpense(transaction);

            Toast.makeText(requireContext(), "Transaction added!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showAddGroupExpenseDialog() {
        if (currentGroups.isEmpty()) {
            Toast.makeText(requireContext(), "You are not part of any groups", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentGroups.size() == 1) {
            showGroupExpenseDialogForGroup(currentGroups.get(0));
        } else {
            CharSequence[] groupNames = new CharSequence[currentGroups.size()];
            for (int i = 0; i < currentGroups.size(); i++) {
                groupNames[i] = currentGroups.get(i).getName();
            }
            new AlertDialog.Builder(requireContext(), R.style.Theme_FairShare_Dialog)
                    .setTitle("Select Group")
                    .setItems(groupNames, (dialog, which) -> {
                        showGroupExpenseDialogForGroup(currentGroups.get(which));
                    })
                    .show();
        }
    }

    private void showGroupExpenseDialogForGroup(com.example.fairshare.Group group) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_group_expense, null);
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_FairShare_Dialog);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        android.widget.TextView tvSelectGroup = dialogView.findViewById(R.id.tvSelectGroup);
        tvSelectGroup.setText(group.getName());
        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        Spinner spinnerWhoPaid = dialogView.findViewById(R.id.spinnerWhoPaid);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        android.widget.LinearLayout containerParticipants = dialogView.findViewById(R.id.containerParticipants);
        MaterialButton btnAddExpense = dialogView.findViewById(R.id.btnAddExpense);
        android.widget.TextView tvParticipatedHeader = dialogView.findViewById(R.id.tvParticipatedHeader);

        dialogView.findViewById(R.id.btnEqualSplit).setOnClickListener(v -> Toast.makeText(requireContext(), "Equal split selected", Toast.LENGTH_SHORT).show());
        dialogView.findViewById(R.id.btnItemized).setOnClickListener(v -> Toast.makeText(requireContext(), "Itemized split coming soon!", Toast.LENGTH_SHORT).show());

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        List<String> memberUids = new ArrayList<>();
        List<String> memberNames = new ArrayList<>();
        java.util.Map<String, android.widget.CheckBox> checkboxes = new java.util.HashMap<>();

        viewModel.getGroupMembers(group.getId(), uids -> {
            for (String uid : uids) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
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

                                ArrayAdapter<String> payerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, memberNames);
                                payerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerWhoPaid.setAdapter(payerAdapter);

                                android.widget.CheckBox cb = new android.widget.CheckBox(requireContext());
                                cb.setText(name);
                                cb.setChecked(true);
                                cb.setTextColor(android.graphics.Color.parseColor("#2D3142"));
                                cb.setPadding(16, 24, 16, 24);
                                cb.setTextSize(16);
                                
                                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                     int count = 0;
                                     for(android.widget.CheckBox c : checkboxes.values()) {
                                         if(c.isChecked()) count++;
                                     }
                                     tvParticipatedHeader.setText("Who Participated? (" + count + " selected)");
                                });
                                containerParticipants.addView(cb);
                                checkboxes.put(uid, cb);
                                
                                tvParticipatedHeader.setText("Who Participated? (" + checkboxes.size() + " selected)");
                                
                                // Set initial payer to "You" if possible
                                if (name.equals("You")) {
                                    spinnerWhoPaid.setSelection(memberNames.size() - 1);
                                }
                            }
                        });
            }
        });

        btnAddExpense.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

            if (title.isEmpty()) { etTitle.setError("Title is required"); return; }
            if (amountStr.isEmpty()) { etAmount.setError("Amount is required"); return; }

            double amount;
            try { amount = Double.parseDouble(amountStr); } 
            catch (NumberFormatException e) { etAmount.setError("Invalid amount"); return; }

            int selectedPayerIndex = spinnerWhoPaid.getSelectedItemPosition();
            if (selectedPayerIndex < 0) { 
                Toast.makeText(requireContext(), "Please select who paid", Toast.LENGTH_SHORT).show(); return; 
            }

            String payerUid = memberUids.get(selectedPayerIndex);
            String payerName = memberNames.get(selectedPayerIndex);

            GroupExpense expense = new GroupExpense(group.getId(), title, payerUid, payerName, amount);
            for (String uid : checkboxes.keySet()) {
                if (checkboxes.get(uid).isChecked()) {
                    expense.getParticipants().add(uid);
                }
            }
            
            if (expense.getParticipants().isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one participant", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.addGroupExpense(group.getId(), expense);
            Toast.makeText(requireContext(), "Group Expense added!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private void showDeleteConfirmation(Transaction transaction) {
        new AlertDialog.Builder(requireContext(), R.style.Theme_FairShare_Dialog)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_msg)
                .setPositiveButton(R.string.delete, (d, which) -> {
                    if (transaction.getId() != null) {
                        viewModel.deletePersonalExpense(transaction.getId());
                        Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCreateGroupDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_group, null);
        TextInputEditText etGroupName = dialogView.findViewById(R.id.etGroupName);
        MaterialButton btnCreate = dialogView.findViewById(R.id.btnCreate);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        Dialog dialog = new Dialog(requireContext(), R.style.Theme_FairShare_Dialog);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreate.setOnClickListener(v -> {
            String name = etGroupName.getText() != null ? etGroupName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                etGroupName.setError("Group name is required");
                return;
            }

            viewModel.createGroup(name, new GroupRepository.OnCompleteCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(requireContext(), "Group created! Code: " + message, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
