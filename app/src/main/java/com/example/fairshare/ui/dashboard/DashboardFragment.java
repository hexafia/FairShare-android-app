package com.example.fairshare.ui.dashboard;

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

import com.example.fairshare.ExpenseAdapter;
import com.example.fairshare.ExpenseViewModel;
import com.example.fairshare.R;
import com.example.fairshare.Transaction;
import com.example.fairshare.databinding.FragmentDashboardBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private ExpenseViewModel viewModel;
    private ExpenseAdapter adapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

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

        binding.fabAdd.setOnClickListener(v -> showAddDialog());
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(this::showDeleteConfirmation);
        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExpenses.setAdapter(adapter);
    }

    private void setupViewModel() {
        // Shared ViewModel tied to Activity
        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
        viewModel.getExpenses().observe(getViewLifecycleOwner(), transactions -> {
            adapter.submitList(transactions);
            updateSummary(transactions);

            if (transactions == null || transactions.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.rvExpenses.setVisibility(View.GONE);
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                binding.rvExpenses.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateSummary(List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;

        if (transactions != null) {
            for (Transaction t : transactions) {
                if ("income".equalsIgnoreCase(t.getType())) {
                    totalIncome += t.getAmount();
                } else {
                    totalExpense += t.getAmount();
                }
            }
        }

        double balance = totalIncome - totalExpense;
        binding.tvBalance.setText(currencyFormat.format(balance));
        binding.tvIncome.setText("+ " + currencyFormat.format(totalIncome));
        binding.tvExpense.setText("- " + currencyFormat.format(totalExpense));
    }

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

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.Theme_FairShare_Dialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();

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
            viewModel.addExpense(transaction);

            Toast.makeText(requireContext(), "Transaction added!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDeleteConfirmation(Transaction transaction) {
        new AlertDialog.Builder(requireContext(), R.style.Theme_FairShare_Dialog)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_msg)
                .setPositiveButton(R.string.delete, (d, which) -> {
                    if (transaction.getId() != null) {
                        viewModel.deleteExpense(transaction.getId());
                        Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
