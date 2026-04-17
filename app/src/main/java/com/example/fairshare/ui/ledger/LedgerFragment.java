package com.example.fairshare.ui.ledger;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.CurrencyHelper;
import com.example.fairshare.ExpenseAdapter;
import com.example.fairshare.ExpenseViewModel;
import com.example.fairshare.R;
import com.example.fairshare.Transaction;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LedgerFragment extends Fragment implements com.example.fairshare.FastActionHandler {

    private ExpenseViewModel viewModel;
    private ExpenseAdapter adapter;

    private List<Transaction> allExpenses = new ArrayList<>();
    private double currentTotalExpense = 0.0;
    
    private TextInputEditText etSearch;
    private Spinner spinnerFilterCategory;
    private MaterialButton btnFilterDate;
    private TextView tvTotalExpense;
    private TextView tvEmptyState;
    private RecyclerView rvPersonalExpenses;

    private String currentSearch = "";
    private String currentCategory = "All";
    private Long selectedDateStart = null; // Storing date as midnight timestamp

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ledger, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.etSearch);
        spinnerFilterCategory = view.findViewById(R.id.spinnerFilterCategory);
        btnFilterDate = view.findViewById(R.id.btnFilterDate);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        rvPersonalExpenses = view.findViewById(R.id.rvPersonalExpenses);

        setupRecyclerView();
        setupFilters();
        setupViewModel();
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(transaction -> {
            // No-op or provide delete capability
            if (transaction.getId() != null) {
                viewModel.deleteExpense(transaction.getId());
            }
        });
        rvPersonalExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPersonalExpenses.setAdapter(adapter);
    }

    private void setupFilters() {
        // Setup Category Spinner
        List<String> categories = new ArrayList<>();
        categories.add("All");
        // Add predefined categories from resources
        String[] arr = getResources().getStringArray(R.array.categories);
        for(String s : arr) categories.add(s);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(catAdapter);

        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories.get(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Setup Search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().trim().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup Date Picker
        btnFilterDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setSelection(selectedDateStart != null ? selectedDateStart : MaterialDatePicker.todayInUtcMilliseconds())
                    .setNegativeButtonText("All Dates")
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDateStart = selection;
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                btnFilterDate.setText(sdf.format(new Date(selection)));
                applyFilters();
            });

            datePicker.addOnNegativeButtonClickListener(dialog -> {
                selectedDateStart = null;
                btnFilterDate.setText("All Dates");
                applyFilters();
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });
    }

    private void setupViewModel() {
        // Shared ViewModel tied to Activity
        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
        viewModel.getExpenses().observe(getViewLifecycleOwner(), transactions -> {
            allExpenses = transactions != null ? transactions : new ArrayList<>();
            applyFilters();
        });

        viewModel.getExpenseTotal().observe(getViewLifecycleOwner(), total -> {
            currentTotalExpense = total != null ? total : 0.0;
            tvTotalExpense.setText(CurrencyHelper.format(currentTotalExpense));
        });
    }

    @Override
    public void onFastAction() {
        if (selectedDateStart != null) {
            selectedDateStart = null;
            btnFilterDate.setText("All Dates");
            applyFilters();
            Toast.makeText(requireContext(), "Date filter cleared", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "No date filter active", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyFilters() {
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction t : allExpenses) {
            boolean matchesSearch = currentSearch.isEmpty() || 
                    (t.getTitle() != null && t.getTitle().toLowerCase().contains(currentSearch));
            
            boolean matchesCategory = "All".equals(currentCategory) || 
                    (t.getCategory() != null && t.getCategory().equals(currentCategory));
            
            boolean matchesDate = true;
            if (selectedDateStart != null && t.getDate() != null) {
                Calendar tCal = Calendar.getInstance();
                tCal.setTime(t.getDate());
                
                Calendar sCal = Calendar.getInstance();
                sCal.setTimeInMillis(selectedDateStart);
                
                matchesDate = tCal.get(Calendar.YEAR) == sCal.get(Calendar.YEAR) &&
                              tCal.get(Calendar.DAY_OF_YEAR) == sCal.get(Calendar.DAY_OF_YEAR);
            }

            if (matchesSearch && matchesCategory && matchesDate) {
                filtered.add(t);
            }
        }

        adapter.submitList(filtered);
        // REMOVED: tvTotalExpense.setText(CurrencyHelper.format(totalFilteredExpense));
        // Total expense is now static and only updated from Firestore observer

        if (filtered.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvPersonalExpenses.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvPersonalExpenses.setVisibility(View.VISIBLE);
        }
    }
}
