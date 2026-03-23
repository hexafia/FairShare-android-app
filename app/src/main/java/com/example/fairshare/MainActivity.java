package com.example.fairshare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fairshare.databinding.ActivityMainBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ExpenseViewModel viewModel;
    private ExpenseAdapter adapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupRecyclerView();
        setupViewModel();

        binding.fabAdd.setOnClickListener(v -> showAddDialog());
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(transaction -> showDeleteConfirmation(transaction));
        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvExpenses.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        viewModel.getExpenses().observe(this, transactions -> {
            adapter.submitList(transactions);
            updateSummary(transactions);

            // Toggle empty state
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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);

        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        MaterialButtonToggleGroup toggleType = dialogView.findViewById(R.id.toggleType);
        MaterialButton btnExpense = dialogView.findViewById(R.id.btnExpense);
        MaterialButton btnIncome = dialogView.findViewById(R.id.btnIncome);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Set up category spinner
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                this, R.array.categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Default selection: Expense
        toggleType.check(R.id.btnExpense);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_FairShare_Dialog)
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

            Toast.makeText(this, "Transaction added!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDeleteConfirmation(Transaction transaction) {
        new AlertDialog.Builder(this, R.style.Theme_FairShare_Dialog)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_msg)
                .setPositiveButton(R.string.delete, (d, which) -> {
                    if (transaction.getId() != null) {
                        viewModel.deleteExpense(transaction.getId());
                        Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}